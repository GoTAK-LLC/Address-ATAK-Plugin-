package com.gotak.address.search;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.atakmap.coremap.log.Log;
import com.gotak.address.search.nearby.OverpassSearchResult;
import com.gotak.address.search.nearby.PointOfInterestType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages offline address databases for state-level geocoding and POI search.
 * Uses SQLite with FTS5 for fast full-text search and R*Tree for spatial POI queries.
 * 
 * Database schema v2 (created by build_state_db.py):
 * - places: Main table with lat, lon, name, display_name, type, etc.
 * - places_fts: FTS5 virtual table for full-text search
 * - pois: POI table with category, name, address, etc.
 * - pois_rtree: R*Tree spatial index for radius queries
 */
public class OfflineAddressDatabase {
    private static final String TAG = "OfflineAddressDatabase";
    private static final int DEFAULT_LIMIT = 10;
    private static final int POI_LIMIT = 100;
    
    private final File databaseDir;
    private SQLiteDatabase currentDb;
    private String currentState;
    
    // Use ATAK's tools directory for easy access
    private static final String ATAK_ADDRESS_DIR = "/sdcard/atak/tools/address";
    
    public OfflineAddressDatabase(Context context) {
        // Store databases in ATAK's tools directory for easy file management
        this.databaseDir = new File(ATAK_ADDRESS_DIR);
        if (!databaseDir.exists()) {
            boolean created = databaseDir.mkdirs();
            Log.d(TAG, "Created database directory: " + databaseDir.getPath() + " success=" + created);
        }
    }
    
    /**
     * Get the directory where offline databases are stored.
     */
    public File getDatabaseDir() {
        return databaseDir;
    }
    
    /**
     * Get the file path for a state's database.
     */
    public File getDatabaseFile(String stateId) {
        return new File(databaseDir, stateId + ".db");
    }
    
    /**
     * Check if a state's database is downloaded.
     */
    public boolean isStateDownloaded(String stateId) {
        return getDatabaseFile(stateId).exists();
    }
    
    /**
     * Get list of downloaded states.
     */
    public List<String> getDownloadedStates() {
        List<String> states = new ArrayList<>();
        File[] files = databaseDir.listFiles((dir, name) -> name.endsWith(".db"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                states.add(name.substring(0, name.length() - 3)); // Remove .db
            }
        }
        return states;
    }
    
    /**
     * Delete a downloaded state database.
     */
    public boolean deleteState(String stateId) {
        // Close if it's the current database
        if (stateId.equals(currentState)) {
            close();
        }
        
        File dbFile = getDatabaseFile(stateId);
        if (dbFile.exists()) {
            return dbFile.delete();
        }
        return false;
    }
    
    /**
     * Open a state's database for searching.
     */
    public boolean openState(String stateId) {
        // Already open
        if (stateId.equals(currentState) && currentDb != null && currentDb.isOpen()) {
            return true;
        }
        
        // Close previous database
        close();
        
        File dbFile = getDatabaseFile(stateId);
        if (!dbFile.exists()) {
            Log.w(TAG, "Database not found: " + dbFile.getPath());
            return false;
        }
        
        try {
            currentDb = SQLiteDatabase.openDatabase(
                    dbFile.getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
            );
            currentState = stateId;
            Log.i(TAG, "Opened database: " + stateId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open database: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Search all downloaded states for matching places.
     */
    public List<NominatimSearchResult> searchAllStates(String query) {
        List<NominatimSearchResult> allResults = new ArrayList<>();
        
        for (String state : getDownloadedStates()) {
            if (openState(state)) {
                List<NominatimSearchResult> stateResults = search(query, DEFAULT_LIMIT);
                allResults.addAll(stateResults);
            }
        }
        
        // Sort by relevance and limit total results
        // (FTS5 results are already ranked, but we should limit total)
        if (allResults.size() > DEFAULT_LIMIT) {
            return allResults.subList(0, DEFAULT_LIMIT);
        }
        
        return allResults;
    }
    
    /**
     * Search the current database for places matching the query.
     * Uses FTS5 full-text search with ranking.
     */
    public List<NominatimSearchResult> search(String query, int limit) {
        List<NominatimSearchResult> results = new ArrayList<>();
        
        if (currentDb == null || !currentDb.isOpen()) {
            Log.w(TAG, "No database open for search");
            return results;
        }
        
        // Sanitize query for FTS5
        String ftsQuery = sanitizeFtsQuery(query);
        if (ftsQuery.isEmpty()) {
            return results;
        }
        
        Cursor cursor = null;
        try {
            // Use FTS5 with BM25 ranking for relevance
            // Match against name, display_name, street, city, postcode
            String sql = 
                "SELECT p.id, p.osm_id, p.osm_type, p.lat, p.lon, " +
                "       p.name, p.display_name, p.type " +
                "FROM places_fts " +
                "JOIN places p ON places_fts.rowid = p.id " +
                "WHERE places_fts MATCH ? " +
                "ORDER BY bm25(places_fts) " +
                "LIMIT ?";
            
            cursor = currentDb.rawQuery(sql, new String[]{ftsQuery, String.valueOf(limit)});
            
            while (cursor.moveToNext()) {
                NominatimSearchResult result = cursorToResult(cursor);
                if (result != null) {
                    results.add(result);
                }
            }
            
            Log.d(TAG, "Offline search '" + query + "' found " + results.size() + " results");
            
        } catch (Exception e) {
            Log.e(TAG, "Search error: " + e.getMessage(), e);
            
            // Try simpler LIKE query as fallback
            results = searchWithLike(query, limit);
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return results;
    }
    
    /**
     * Search a specific state's database for places matching the query.
     * 
     * @param stateId The state database ID (e.g., "arkansas", "new-york")
     * @param query The search query
     * @param limit Maximum number of results
     * @return List of matching results, or empty list if state not available
     */
    public List<NominatimSearchResult> searchState(String stateId, String query, int limit) {
        if (!openState(stateId)) {
            Log.w(TAG, "Could not open state database: " + stateId);
            return new ArrayList<>();
        }
        return search(query, limit);
    }
    
    /**
     * Search a specific state's database for places and POIs matching a name.
     * This is useful for queries like "walmart arkansas" where we want to find
     * both address entries and POIs with that name.
     * 
     * @param stateId The state database ID (e.g., "arkansas", "new-york")
     * @param name The name to search for (e.g., "walmart", "target")
     * @return Combined list of matching places and POIs
     */
    public List<NominatimSearchResult> searchStateByName(String stateId, String name) {
        List<NominatimSearchResult> results = new ArrayList<>();
        
        if (!openState(stateId)) {
            Log.w(TAG, "Could not open state database: " + stateId);
            return results;
        }
        
        // Search places table
        results.addAll(search(name, DEFAULT_LIMIT));
        
        // Also search POIs by name if the table exists
        if (hasPOIData()) {
            List<NominatimSearchResult> poiResults = searchPOIsByName(name, DEFAULT_LIMIT);
            results.addAll(poiResults);
        }
        
        // Remove duplicates (based on osm_id) and limit total
        List<NominatimSearchResult> uniqueResults = new ArrayList<>();
        java.util.Set<Long> seenIds = new java.util.HashSet<>();
        for (NominatimSearchResult r : results) {
            if (!seenIds.contains(r.getOsmId())) {
                seenIds.add(r.getOsmId());
                uniqueResults.add(r);
            }
            if (uniqueResults.size() >= DEFAULT_LIMIT) break;
        }
        
        return uniqueResults;
    }
    
    /**
     * Search POIs by name within the current database.
     * Used for queries like "walmart" or "starbucks" within a specific state.
     */
    private List<NominatimSearchResult> searchPOIsByName(String name, int limit) {
        List<NominatimSearchResult> results = new ArrayList<>();
        
        if (currentDb == null || !currentDb.isOpen()) {
            return results;
        }
        
        Cursor cursor = null;
        try {
            String likeQuery = "%" + name.replace("%", "").replace("_", "") + "%";
            
            String sql = 
                "SELECT id, osm_id, osm_type, lat, lon, name, category, address " +
                "FROM pois " +
                "WHERE name LIKE ? " +
                "ORDER BY name " +
                "LIMIT ?";
            
            cursor = currentDb.rawQuery(sql, new String[]{likeQuery, String.valueOf(limit)});
            
            while (cursor.moveToNext()) {
                try {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    long osmId = cursor.getLong(cursor.getColumnIndexOrThrow("osm_id"));
                    String osmType = cursor.getString(cursor.getColumnIndexOrThrow("osm_type"));
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
                    String poiName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    
                    // Build display name from POI info
                    String displayName = poiName;
                    if (address != null && !address.isEmpty()) {
                        displayName = poiName + ", " + address;
                    }
                    
                    // Map category to a type string
                    String type = category != null ? category.toLowerCase().replace("_", " ") : "poi";
                    
                    results.add(new NominatimSearchResult(
                            id, lat, lon, displayName, poiName, type, osmType, osmId
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing POI result: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "POI name search '" + name + "' found " + results.size() + " results");
            
        } catch (Exception e) {
            Log.e(TAG, "POI name search error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return results;
    }
    
    /**
     * Search all POIs within a specific state by category.
     * Unlike searchPOIs which searches by location, this searches the entire state.
     * 
     * @param stateId The state database ID
     * @param categories POI categories to search for
     * @param limit Maximum number of results
     * @return List of POI results
     */
    public List<OverpassSearchResult> searchStatePOIsByCategory(
            String stateId, java.util.Set<PointOfInterestType> categories, int limit) {
        
        List<OverpassSearchResult> results = new ArrayList<>();
        
        if (!openState(stateId) || !hasPOIData()) {
            return results;
        }
        
        if (categories == null || categories.isEmpty()) {
            return results;
        }
        
        // Build category filter
        StringBuilder categoryFilter = new StringBuilder();
        List<String> categoryNames = new ArrayList<>();
        for (PointOfInterestType type : categories) {
            if (categoryFilter.length() > 0) {
                categoryFilter.append(" OR ");
            }
            categoryFilter.append("category = ?");
            categoryNames.add(type.name());
        }
        
        Cursor cursor = null;
        try {
            String sql = 
                "SELECT id, osm_id, osm_type, lat, lon, name, category, address, phone, website, opening_hours " +
                "FROM pois " +
                "WHERE " + categoryFilter + " " +
                "ORDER BY name " +
                "LIMIT ?";
            
            String[] args = new String[categoryNames.size() + 1];
            for (int i = 0; i < categoryNames.size(); i++) {
                args[i] = categoryNames.get(i);
            }
            args[args.length - 1] = String.valueOf(limit);
            
            cursor = currentDb.rawQuery(sql, args);
            
            while (cursor.moveToNext()) {
                try {
                    long osmId = cursor.getLong(cursor.getColumnIndexOrThrow("osm_id"));
                    String osmType = cursor.getString(cursor.getColumnIndexOrThrow("osm_type"));
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String categoryStr = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    
                    PointOfInterestType poiType = null;
                    try {
                        poiType = PointOfInterestType.valueOf(categoryStr);
                    } catch (IllegalArgumentException ignored) {}
                    
                    // No distance calculation since we're not searching by location
                    results.add(new OverpassSearchResult(
                        osmId, osmType, name, lat, lon, 0, poiType, address, null
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing POI result: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "State POI category search found " + results.size() + " results in " + stateId);
            
        } catch (Exception e) {
            Log.e(TAG, "State POI category search error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return results;
    }
    
    // ============ POI SPATIAL SEARCH ============
    
    /**
     * Check if the database has POI data (schema version 2+).
     */
    public boolean hasPOIData() {
        if (currentDb == null || !currentDb.isOpen()) {
            return false;
        }
        
        Cursor cursor = null;
        try {
            // Check if pois table exists
            cursor = currentDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='pois'", null);
            return cursor.moveToFirst();
        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
    
    /**
     * Search all downloaded states for POIs within a radius.
     * 
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm Radius in kilometers
     * @param categories POI categories to search for (from PointOfInterestType)
     * @return List of POI results sorted by distance
     */
    public List<OverpassSearchResult> searchPOIsAllStates(
            double centerLat, double centerLon, int radiusKm, 
            Set<PointOfInterestType> categories) {
        
        List<OverpassSearchResult> allResults = new ArrayList<>();
        
        for (String state : getDownloadedStates()) {
            if (openState(state) && hasPOIData()) {
                List<OverpassSearchResult> stateResults = searchPOIs(
                    centerLat, centerLon, radiusKm, categories);
                allResults.addAll(stateResults);
            }
        }
        
        // Sort all results by distance and limit
        allResults.sort((a, b) -> Double.compare(a.getDistanceMeters(), b.getDistanceMeters()));
        
        if (allResults.size() > POI_LIMIT) {
            return allResults.subList(0, POI_LIMIT);
        }
        
        return allResults;
    }
    
    /**
     * Search current database for POIs within a radius using R*Tree spatial index.
     * 
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm Radius in kilometers
     * @param categories POI categories to search for
     * @return List of POI results sorted by distance
     */
    public List<OverpassSearchResult> searchPOIs(
            double centerLat, double centerLon, int radiusKm,
            Set<PointOfInterestType> categories) {
        
        List<OverpassSearchResult> results = new ArrayList<>();
        
        if (currentDb == null || !currentDb.isOpen()) {
            Log.w(TAG, "No database open for POI search");
            return results;
        }
        
        if (categories == null || categories.isEmpty()) {
            Log.w(TAG, "No POI categories specified");
            return results;
        }
        
        // Convert radius to approximate degrees for bounding box
        // 1 degree latitude ≈ 111 km
        // 1 degree longitude varies by latitude, but we use a conservative estimate
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        
        double minLat = centerLat - latDelta;
        double maxLat = centerLat + latDelta;
        double minLon = centerLon - lonDelta;
        double maxLon = centerLon + lonDelta;
        
        // Build category filter
        StringBuilder categoryFilter = new StringBuilder();
        List<String> categoryNames = new ArrayList<>();
        for (PointOfInterestType type : categories) {
            if (categoryFilter.length() > 0) {
                categoryFilter.append(" OR ");
            }
            categoryFilter.append("p.category = ?");
            categoryNames.add(type.name());
        }
        
        Cursor cursor = null;
        try {
            // Use R*Tree for spatial filtering, then filter by category
            // The R*Tree dramatically reduces the search space
            String sql = 
                "SELECT p.id, p.osm_id, p.osm_type, p.lat, p.lon, " +
                "       p.name, p.category, p.address, p.phone, p.website, p.opening_hours " +
                "FROM pois p " +
                "INNER JOIN pois_rtree r ON p.id = r.id " +
                "WHERE r.min_lat >= ? AND r.max_lat <= ? " +
                "  AND r.min_lon >= ? AND r.max_lon <= ? " +
                "  AND (" + categoryFilter + ") " +
                "LIMIT ?";
            
            // Build args array
            String[] args = new String[4 + categoryNames.size() + 1];
            args[0] = String.valueOf(minLat);
            args[1] = String.valueOf(maxLat);
            args[2] = String.valueOf(minLon);
            args[3] = String.valueOf(maxLon);
            for (int i = 0; i < categoryNames.size(); i++) {
                args[4 + i] = categoryNames.get(i);
            }
            args[args.length - 1] = String.valueOf(POI_LIMIT);
            
            cursor = currentDb.rawQuery(sql, args);
            
            double radiusMeters = radiusKm * 1000.0;
            
            while (cursor.moveToNext()) {
                OverpassSearchResult result = cursorToPOIResult(cursor, centerLat, centerLon);
                if (result != null) {
                    // Final distance check (bounding box may include corners outside radius)
                    if (result.getDistanceMeters() <= radiusMeters) {
                        results.add(result);
                    }
                }
            }
            
            // Sort by distance
            results.sort((a, b) -> Double.compare(a.getDistanceMeters(), b.getDistanceMeters()));
            
            Log.d(TAG, "Offline POI search found " + results.size() + " results within " + radiusKm + " km");
            
        } catch (Exception e) {
            Log.e(TAG, "POI search error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return results;
    }
    
    /**
     * Convert a cursor row to an OverpassSearchResult.
     */
    private OverpassSearchResult cursorToPOIResult(Cursor cursor, double centerLat, double centerLon) {
        try {
            long osmId = cursor.getLong(cursor.getColumnIndexOrThrow("osm_id"));
            String osmType = cursor.getString(cursor.getColumnIndexOrThrow("osm_type"));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String categoryStr = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            
            // Calculate distance
            double distance = calculateDistance(centerLat, centerLon, lat, lon);
            
            // Map category string back to enum
            PointOfInterestType poiType = null;
            try {
                poiType = PointOfInterestType.valueOf(categoryStr);
            } catch (IllegalArgumentException ignored) {}
            
            return new OverpassSearchResult(
                osmId, osmType, name, lat, lon, distance, poiType, address, null
            );
        } catch (Exception e) {
            Log.e(TAG, "Error parsing POI result: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate distance between two points using the Haversine formula.
     * @return Distance in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
    
    // ============ EXISTING METHODS ============
    
    /**
     * Fallback search using LIKE (slower but more compatible).
     */
    private List<NominatimSearchResult> searchWithLike(String query, int limit) {
        List<NominatimSearchResult> results = new ArrayList<>();
        
        if (currentDb == null || !currentDb.isOpen()) {
            return results;
        }
        
        Cursor cursor = null;
        try {
            String likeQuery = "%" + query.replace("%", "").replace("_", "") + "%";
            
            String sql = 
                "SELECT id, osm_id, osm_type, lat, lon, name, display_name, type " +
                "FROM places " +
                "WHERE name LIKE ? OR display_name LIKE ? OR street LIKE ? OR city LIKE ? " +
                "LIMIT ?";
            
            cursor = currentDb.rawQuery(sql, new String[]{
                    likeQuery, likeQuery, likeQuery, likeQuery, String.valueOf(limit)
            });
            
            while (cursor.moveToNext()) {
                NominatimSearchResult result = cursorToResult(cursor);
                if (result != null) {
                    results.add(result);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "LIKE search error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return results;
    }
    
    /**
     * Convert a cursor row to a NominatimSearchResult.
     */
    private NominatimSearchResult cursorToResult(Cursor cursor) {
        try {
            long placeId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            long osmId = cursor.getLong(cursor.getColumnIndexOrThrow("osm_id"));
            String osmType = cursor.getString(cursor.getColumnIndexOrThrow("osm_type"));
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("lon"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
            String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            
            return new NominatimSearchResult(
                    placeId, lat, lon, displayName, name, type, osmType, osmId
            );
        } catch (Exception e) {
            Log.e(TAG, "Error parsing result: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sanitize a query string for FTS5.
     * - Adds prefix matching (word*)
     * - Escapes special characters
     */
    private String sanitizeFtsQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        
        // Remove FTS5 special characters
        String sanitized = query.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace("*", "")
                .replace("(", "")
                .replace(")", "")
                .replace("-", " ")
                .replace(":", " ");
        
        // Split into words and add prefix matching
        String[] words = sanitized.split("\\s+");
        StringBuilder ftsQuery = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (ftsQuery.length() > 0) {
                    ftsQuery.append(" ");
                }
                // Add prefix matching for partial word search
                ftsQuery.append(word).append("*");
            }
        }
        
        return ftsQuery.toString();
    }
    
    /**
     * Get database statistics for a state.
     */
    public DatabaseStats getStats(String stateId) {
        File dbFile = getDatabaseFile(stateId);
        if (!dbFile.exists()) {
            return null;
        }
        
        DatabaseStats stats = new DatabaseStats();
        stats.stateId = stateId;
        stats.fileSizeBytes = dbFile.length();
        
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // Get place count
            cursor = db.rawQuery("SELECT COUNT(*) FROM places", null);
            if (cursor.moveToFirst()) {
                stats.placeCount = cursor.getInt(0);
            }
            cursor.close();
            
            // Get POI count (if available)
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM pois", null);
                if (cursor.moveToFirst()) {
                    stats.poiCount = cursor.getInt(0);
                }
                cursor.close();
            } catch (Exception ignored) {
                stats.poiCount = 0;
            }
            
            // Get created date from metadata
            cursor = db.rawQuery(
                    "SELECT value FROM metadata WHERE key = 'created'", null);
            if (cursor.moveToFirst()) {
                stats.createdDate = cursor.getString(0);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting stats: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        
        return stats;
    }
    
    /**
     * Close the current database.
     */
    public void close() {
        if (currentDb != null) {
            try {
                currentDb.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing database: " + e.getMessage());
            }
            currentDb = null;
            currentState = null;
        }
    }
    
    /**
     * Database statistics.
     */
    public static class DatabaseStats {
        public String stateId;
        public long fileSizeBytes;
        public int placeCount;
        public int poiCount;
        public String createdDate;
        
        public String getFileSizeFormatted() {
            if (fileSizeBytes < 1024) {
                return fileSizeBytes + " B";
            } else if (fileSizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", fileSizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
            }
        }
    }
}
