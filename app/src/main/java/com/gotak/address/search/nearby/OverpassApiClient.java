package com.gotak.address.search.nearby;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.atakmap.coremap.log.Log;
import com.gotak.address.search.OfflineAddressDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for querying Points of Interest.
 * 
 * Search priority:
 * 1. Offline database (if downloaded) - instant results, works without internet
 * 2. OpenStreetMap Overpass API (online)
 * 
 * Uses asynchronous requests with callback interface for results.
 */
public class OverpassApiClient {

    private static final String TAG = "OverpassApiClient";
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    private static final int TIMEOUT_MS = 30000;
    private static final int MIN_OFFLINE_RESULTS = 10;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 3000;

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final OfflineAddressDatabase offlineDatabase;
    private boolean offlineOnly = false;

    public OverpassApiClient(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.offlineDatabase = new OfflineAddressDatabase(context);
    }

    /**
     * Set whether to use offline-only mode (no network requests).
     */
    public void setOfflineOnly(boolean offlineOnly) {
        this.offlineOnly = offlineOnly;
    }

    /**
     * Check if device has network connectivity.
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking network: " + e.getMessage());
        }
        return true; // Assume available on error
    }

    /**
     * Callback interface for search results.
     */
    public interface SearchCallback {
        void onSuccess(List<OverpassSearchResult> results);
        void onError(String errorMessage);
    }

    /**
     * Search for POIs of the specified types within a radius around a location.
     * Checks offline database first, then falls back to online API.
     *
     * @param lat The latitude of the center point
     * @param lon The longitude of the center point
     * @param radiusKm The search radius in kilometers
     * @param types The POI types to search for
     * @param callback The callback for results
     */
    public void searchNearby(double lat, double lon, int radiusKm, 
                             List<PointOfInterestType> types, SearchCallback callback) {
        if (types == null || types.isEmpty()) {
            mainHandler.post(() -> callback.onError("No POI categories selected"));
            return;
        }

        executor.execute(() -> {
            List<OverpassSearchResult> results = new ArrayList<>();
            
            // Step 1: Try offline database first
            if (!offlineDatabase.getDownloadedStates().isEmpty()) {
                try {
                    Log.d(TAG, "Searching offline POI database...");
                    Set<PointOfInterestType> typeSet = new HashSet<>(types);
                    results = offlineDatabase.searchPOIsAllStates(lat, lon, radiusKm, typeSet);
                    Log.i(TAG, "Offline POI search found " + results.size() + " results");
                    
                    // If offline-only mode or we have good results, return them
                    if (offlineOnly || results.size() >= MIN_OFFLINE_RESULTS) {
                        final List<OverpassSearchResult> finalResults = results;
                        mainHandler.post(() -> callback.onSuccess(finalResults));
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Offline POI search error: " + e.getMessage());
                }
            }
            
            // Step 2: If offline-only mode, return what we have
            if (offlineOnly) {
                final List<OverpassSearchResult> finalResults = results;
                mainHandler.post(() -> callback.onSuccess(finalResults));
                return;
            }
            
            // Step 3: Check network availability
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No network available, returning offline results only");
                final List<OverpassSearchResult> finalResults = results;
                if (results.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No network connection and no offline data"));
                } else {
                    mainHandler.post(() -> callback.onSuccess(finalResults));
                }
                return;
            }
            
            // Step 4: Try online Overpass API
            try {
                String query = buildOverpassQuery(lat, lon, radiusKm * 1000, types);
                Log.d(TAG, "Overpass query: " + query);
                
                String response = executeQuery(query);
                List<OverpassSearchResult> onlineResults = parseResponse(response, lat, lon);
                
                // Prefer online results if we got any
                if (!onlineResults.isEmpty()) {
                    results = onlineResults;
                }
                
                final List<OverpassSearchResult> finalResults = results;
                mainHandler.post(() -> callback.onSuccess(finalResults));
                
            } catch (Exception e) {
                Log.e(TAG, "Online search error: " + e.getMessage(), e);
                
                // Return offline results if we have any, otherwise report error
                if (!results.isEmpty()) {
                    final List<OverpassSearchResult> finalResults = results;
                    mainHandler.post(() -> callback.onSuccess(finalResults));
                } else {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Build an Overpass QL query for the specified POI types.
     */
    private String buildOverpassQuery(double lat, double lon, int radiusMeters, 
                                       List<PointOfInterestType> types) {
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:25];\n");
        query.append("(\n");
        
        for (PointOfInterestType type : types) {
            query.append(type.toOverpassQueryFragment(lat, lon, radiusMeters));
            query.append("\n");
        }
        
        query.append(");\n");
        query.append("out center;\n");
        
        return query.toString();
    }

    /**
     * Execute the Overpass query and return the raw response.
     * Automatically retries on 502/503/504 errors after a 3-second delay.
     */
    private String executeQuery(String query) throws IOException {
        return executeQueryWithRetry(query, 1);
    }

    /**
     * Execute query with retry logic for gateway errors.
     */
    private String executeQueryWithRetry(String query, int attempt) throws IOException {
        URL url = new URL(OVERPASS_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "ATAK-Address-Plugin/1.0");

            // Write query as POST data
            String postData = "data=" + URLEncoder.encode(query, "UTF-8");
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Retry on 502, 503, or 504 gateway errors
                if (isRetryableError(responseCode) && attempt < MAX_RETRY_ATTEMPTS) {
                    Log.i(TAG, "Got HTTP " + responseCode + ", retrying after " + RETRY_DELAY_MS + 
                          "ms (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
                    connection.disconnect();
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted");
                    }
                    return executeQueryWithRetry(query, attempt + 1);
                }
                throw new IOException("HTTP error code: " + responseCode);
            }

            // Read response
            try (InputStream is = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Check if the HTTP error code is retryable (gateway errors).
     */
    private boolean isRetryableError(int responseCode) {
        return responseCode == 502 || responseCode == 503 || responseCode == 504;
    }

    /**
     * Parse the Overpass JSON response into search results.
     */
    private List<OverpassSearchResult> parseResponse(String jsonResponse, 
                                                      double centerLat, double centerLon) throws JSONException {
        List<OverpassSearchResult> results = new ArrayList<>();
        
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray elements = root.optJSONArray("elements");
        
        if (elements == null) {
            return results;
        }

        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            
            // Get coordinates - for ways, use the center point
            double lat, lon;
            if (element.has("center")) {
                JSONObject center = element.getJSONObject("center");
                lat = center.getDouble("lat");
                lon = center.getDouble("lon");
            } else {
                lat = element.optDouble("lat", 0);
                lon = element.optDouble("lon", 0);
            }
            
            if (lat == 0 && lon == 0) {
                continue; // Skip elements without valid coordinates
            }

            // Get tags
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null) {
                continue;
            }

            // Determine POI type from tags
            PointOfInterestType poiType = determinePoiType(tags);
            
            // Get name
            String name = tags.optString("name", "");
            if (name.isEmpty()) {
                // Try alternative name fields
                name = tags.optString("official_name", "");
                if (name.isEmpty()) {
                    name = tags.optString("alt_name", "");
                }
                if (name.isEmpty() && poiType != null) {
                    // Use POI type as fallback name
                    name = poiType.getOsmValue().replace("_", " ");
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                }
            }

            // Calculate distance from center
            double distance = calculateDistance(centerLat, centerLon, lat, lon);

            // Get additional details
            String address = buildAddress(tags);
            
            long osmId = element.optLong("id", 0);
            String osmType = element.optString("type", "node");

            OverpassSearchResult result = new OverpassSearchResult(
                osmId, osmType, name, lat, lon, distance, poiType, address, tags
            );
            results.add(result);
        }

        // Sort by distance
        results.sort((a, b) -> Double.compare(a.getDistanceMeters(), b.getDistanceMeters()));
        
        return results;
    }

    /**
     * Determine the POI type from OSM tags.
     */
    private PointOfInterestType determinePoiType(JSONObject tags) {
        for (PointOfInterestType type : PointOfInterestType.values()) {
            String value = tags.optString(type.getOsmKey(), "");
            if (type.getOsmValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Build a readable address from OSM tags.
     */
    private String buildAddress(JSONObject tags) {
        StringBuilder address = new StringBuilder();
        
        String street = tags.optString("addr:street", "");
        String houseNumber = tags.optString("addr:housenumber", "");
        String city = tags.optString("addr:city", "");
        String postcode = tags.optString("addr:postcode", "");
        
        if (!houseNumber.isEmpty()) {
            address.append(houseNumber);
        }
        if (!street.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(street);
        }
        if (!city.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (!postcode.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(postcode);
        }
        
        return address.toString();
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

    /**
     * Shutdown the executor service and close offline database.
     */
    public void shutdown() {
        executor.shutdown();
        offlineDatabase.close();
    }
}
