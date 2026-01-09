package com.gotak.address.search;

import com.gotak.address.search.nearby.PointOfInterestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fuzzy matching engine for detecting POI category searches in natural language queries.
 * 
 * Handles queries like:
 * - "gas station near me"
 * - "coffee shop nearby"
 * - "cemetery near"
 * - "hospital"
 * 
 * Uses keyword aliases and fuzzy matching to map user input to POI categories.
 */
public class CategoryMatcher {
    
    // Patterns to detect "near me", "nearby", "near" suffixes
    private static final Pattern NEARBY_PATTERN = Pattern.compile(
            "\\s*(near\\s*me|nearby|near\\s*by|close\\s*by|around\\s*me|near)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    
    // Maximum Levenshtein distance for fuzzy matching (relative to word length)
    private static final double MAX_DISTANCE_RATIO = 0.3; // 30% of word length
    
    // Alias map: lowercase keyword -> POI type
    private static final Map<String, PointOfInterestType> ALIASES = new HashMap<>();
    
    static {
        // Gas Station / Fuel
        ALIASES.put("gas", PointOfInterestType.GAS_STATION);
        ALIASES.put("gas station", PointOfInterestType.GAS_STATION);
        ALIASES.put("gasoline", PointOfInterestType.GAS_STATION);
        ALIASES.put("fuel", PointOfInterestType.GAS_STATION);
        ALIASES.put("petrol", PointOfInterestType.GAS_STATION);
        ALIASES.put("petrol station", PointOfInterestType.GAS_STATION);
        ALIASES.put("filling station", PointOfInterestType.GAS_STATION);
        
        // Hospital / Medical
        ALIASES.put("hospital", PointOfInterestType.HOSPITAL);
        ALIASES.put("hospitals", PointOfInterestType.HOSPITAL);
        ALIASES.put("emergency room", PointOfInterestType.HOSPITAL);
        ALIASES.put("er", PointOfInterestType.HOSPITAL);
        ALIASES.put("medical center", PointOfInterestType.HOSPITAL);
        
        // Pharmacy
        ALIASES.put("pharmacy", PointOfInterestType.PHARMACY);
        ALIASES.put("pharmacies", PointOfInterestType.PHARMACY);
        ALIASES.put("drugstore", PointOfInterestType.PHARMACY);
        ALIASES.put("drug store", PointOfInterestType.PHARMACY);
        ALIASES.put("chemist", PointOfInterestType.PHARMACY);
        
        // Police
        ALIASES.put("police", PointOfInterestType.POLICE_STATION);
        ALIASES.put("police station", PointOfInterestType.POLICE_STATION);
        ALIASES.put("cops", PointOfInterestType.POLICE_STATION);
        ALIASES.put("sheriff", PointOfInterestType.POLICE_STATION);
        
        // Fire Station
        ALIASES.put("fire station", PointOfInterestType.FIRE_STATION);
        ALIASES.put("fire department", PointOfInterestType.FIRE_STATION);
        ALIASES.put("firehouse", PointOfInterestType.FIRE_STATION);
        ALIASES.put("fire brigade", PointOfInterestType.FIRE_STATION);
        
        // Airport
        ALIASES.put("airport", PointOfInterestType.AIRPORT);
        ALIASES.put("airports", PointOfInterestType.AIRPORT);
        ALIASES.put("airfield", PointOfInterestType.AIRPORT);
        
        // Heliport
        ALIASES.put("heliport", PointOfInterestType.HELIPORT);
        ALIASES.put("helipad", PointOfInterestType.HELIPORT);
        ALIASES.put("helicopter", PointOfInterestType.HELIPORT);
        
        // Railway
        ALIASES.put("train station", PointOfInterestType.RAILWAY_STATION);
        ALIASES.put("railway station", PointOfInterestType.RAILWAY_STATION);
        ALIASES.put("train", PointOfInterestType.RAILWAY_STATION);
        ALIASES.put("railway", PointOfInterestType.RAILWAY_STATION);
        ALIASES.put("metro", PointOfInterestType.RAILWAY_STATION);
        ALIASES.put("subway", PointOfInterestType.RAILWAY_STATION);
        
        // Ferry
        ALIASES.put("ferry", PointOfInterestType.FERRY_TERMINAL);
        ALIASES.put("ferry terminal", PointOfInterestType.FERRY_TERMINAL);
        
        // Parking
        ALIASES.put("parking", PointOfInterestType.PARKING);
        ALIASES.put("parking lot", PointOfInterestType.PARKING);
        ALIASES.put("car park", PointOfInterestType.PARKING);
        ALIASES.put("garage", PointOfInterestType.PARKING);
        
        // Bank
        ALIASES.put("bank", PointOfInterestType.BANK);
        ALIASES.put("banks", PointOfInterestType.BANK);
        
        // ATM
        ALIASES.put("atm", PointOfInterestType.ATM);
        ALIASES.put("cash machine", PointOfInterestType.ATM);
        ALIASES.put("cash point", PointOfInterestType.ATM);
        
        // School
        ALIASES.put("school", PointOfInterestType.SCHOOL);
        ALIASES.put("schools", PointOfInterestType.SCHOOL);
        ALIASES.put("elementary school", PointOfInterestType.SCHOOL);
        ALIASES.put("high school", PointOfInterestType.SCHOOL);
        
        // Restaurant
        ALIASES.put("restaurant", PointOfInterestType.RESTAURANT);
        ALIASES.put("restaurants", PointOfInterestType.RESTAURANT);
        ALIASES.put("food", PointOfInterestType.RESTAURANT);
        ALIASES.put("eat", PointOfInterestType.RESTAURANT);
        ALIASES.put("dining", PointOfInterestType.RESTAURANT);
        
        // Hotel
        ALIASES.put("hotel", PointOfInterestType.HOTEL);
        ALIASES.put("hotels", PointOfInterestType.HOTEL);
        ALIASES.put("motel", PointOfInterestType.HOTEL);
        ALIASES.put("lodging", PointOfInterestType.HOTEL);
        ALIASES.put("accommodation", PointOfInterestType.HOTEL);
        ALIASES.put("inn", PointOfInterestType.HOTEL);
        
        // Cafe / Coffee
        ALIASES.put("cafe", PointOfInterestType.CAFE);
        ALIASES.put("cafes", PointOfInterestType.CAFE);
        ALIASES.put("coffee", PointOfInterestType.CAFE);
        ALIASES.put("coffee shop", PointOfInterestType.CAFE);
        ALIASES.put("coffeeshop", PointOfInterestType.CAFE);
        ALIASES.put("starbucks", PointOfInterestType.CAFE);
        ALIASES.put("espresso", PointOfInterestType.CAFE);
        
        // Fast Food
        ALIASES.put("fast food", PointOfInterestType.FAST_FOOD);
        ALIASES.put("fastfood", PointOfInterestType.FAST_FOOD);
        ALIASES.put("mcdonalds", PointOfInterestType.FAST_FOOD);
        ALIASES.put("burger", PointOfInterestType.FAST_FOOD);
        ALIASES.put("burgers", PointOfInterestType.FAST_FOOD);
        ALIASES.put("drive thru", PointOfInterestType.FAST_FOOD);
        ALIASES.put("drive through", PointOfInterestType.FAST_FOOD);
        
        // Bar / Pub
        ALIASES.put("bar", PointOfInterestType.BAR);
        ALIASES.put("bars", PointOfInterestType.BAR);
        ALIASES.put("pub", PointOfInterestType.PUB);
        ALIASES.put("pubs", PointOfInterestType.PUB);
        ALIASES.put("tavern", PointOfInterestType.BAR);
        ALIASES.put("nightclub", PointOfInterestType.BAR);
        ALIASES.put("drinks", PointOfInterestType.BAR);
        
        // Supermarket / Grocery
        ALIASES.put("supermarket", PointOfInterestType.SUPERMARKET);
        ALIASES.put("supermarkets", PointOfInterestType.SUPERMARKET);
        ALIASES.put("grocery", PointOfInterestType.SUPERMARKET);
        ALIASES.put("grocery store", PointOfInterestType.SUPERMARKET);
        ALIASES.put("groceries", PointOfInterestType.SUPERMARKET);
        ALIASES.put("food store", PointOfInterestType.SUPERMARKET);
        ALIASES.put("market", PointOfInterestType.SUPERMARKET);
        
        // Convenience Store
        ALIASES.put("convenience store", PointOfInterestType.CONVENIENCE_STORE);
        ALIASES.put("convenience", PointOfInterestType.CONVENIENCE_STORE);
        ALIASES.put("corner store", PointOfInterestType.CONVENIENCE_STORE);
        ALIASES.put("7-eleven", PointOfInterestType.CONVENIENCE_STORE);
        ALIASES.put("7 eleven", PointOfInterestType.CONVENIENCE_STORE);
        
        // Shopping Mall
        ALIASES.put("mall", PointOfInterestType.SHOPPING_MALL);
        ALIASES.put("shopping mall", PointOfInterestType.SHOPPING_MALL);
        ALIASES.put("shopping center", PointOfInterestType.SHOPPING_MALL);
        ALIASES.put("shopping centre", PointOfInterestType.SHOPPING_MALL);
        
        // Hardware Store
        ALIASES.put("hardware store", PointOfInterestType.HARDWARE_STORE);
        ALIASES.put("hardware", PointOfInterestType.HARDWARE_STORE);
        ALIASES.put("home depot", PointOfInterestType.HARDWARE_STORE);
        ALIASES.put("lowes", PointOfInterestType.HARDWARE_STORE);
        
        // Dentist
        ALIASES.put("dentist", PointOfInterestType.DENTIST);
        ALIASES.put("dentists", PointOfInterestType.DENTIST);
        ALIASES.put("dental", PointOfInterestType.DENTIST);
        ALIASES.put("dental office", PointOfInterestType.DENTIST);
        
        // Doctor
        ALIASES.put("doctor", PointOfInterestType.DOCTOR);
        ALIASES.put("doctors", PointOfInterestType.DOCTOR);
        ALIASES.put("physician", PointOfInterestType.DOCTOR);
        ALIASES.put("medical office", PointOfInterestType.DOCTOR);
        ALIASES.put("gp", PointOfInterestType.DOCTOR);
        
        // Clinic
        ALIASES.put("clinic", PointOfInterestType.CLINIC);
        ALIASES.put("clinics", PointOfInterestType.CLINIC);
        ALIASES.put("urgent care", PointOfInterestType.CLINIC);
        ALIASES.put("walk in clinic", PointOfInterestType.CLINIC);
        
        // Veterinarian
        ALIASES.put("vet", PointOfInterestType.VETERINARIAN);
        ALIASES.put("vets", PointOfInterestType.VETERINARIAN);
        ALIASES.put("veterinarian", PointOfInterestType.VETERINARIAN);
        ALIASES.put("veterinary", PointOfInterestType.VETERINARIAN);
        ALIASES.put("animal hospital", PointOfInterestType.VETERINARIAN);
        ALIASES.put("pet doctor", PointOfInterestType.VETERINARIAN);
        
        // Car Wash
        ALIASES.put("car wash", PointOfInterestType.CAR_WASH);
        ALIASES.put("carwash", PointOfInterestType.CAR_WASH);
        ALIASES.put("auto wash", PointOfInterestType.CAR_WASH);
        
        // Laundry
        ALIASES.put("laundry", PointOfInterestType.LAUNDRY);
        ALIASES.put("laundromat", PointOfInterestType.LAUNDRY);
        ALIASES.put("launderette", PointOfInterestType.LAUNDRY);
        ALIASES.put("dry cleaner", PointOfInterestType.LAUNDRY);
        ALIASES.put("dry cleaning", PointOfInterestType.LAUNDRY);
        
        // Hair Salon
        ALIASES.put("hair salon", PointOfInterestType.HAIR_SALON);
        ALIASES.put("hairdresser", PointOfInterestType.HAIR_SALON);
        ALIASES.put("barber", PointOfInterestType.HAIR_SALON);
        ALIASES.put("barbershop", PointOfInterestType.HAIR_SALON);
        ALIASES.put("haircut", PointOfInterestType.HAIR_SALON);
        ALIASES.put("salon", PointOfInterestType.HAIR_SALON);
        
        // Cinema / Movie Theater
        ALIASES.put("cinema", PointOfInterestType.CINEMA);
        ALIASES.put("movie theater", PointOfInterestType.CINEMA);
        ALIASES.put("movie theatre", PointOfInterestType.CINEMA);
        ALIASES.put("movies", PointOfInterestType.CINEMA);
        ALIASES.put("theater", PointOfInterestType.CINEMA);
        ALIASES.put("theatre", PointOfInterestType.CINEMA);
        
        // Gym / Fitness
        ALIASES.put("gym", PointOfInterestType.GYM);
        ALIASES.put("gyms", PointOfInterestType.GYM);
        ALIASES.put("fitness", PointOfInterestType.GYM);
        ALIASES.put("fitness center", PointOfInterestType.GYM);
        ALIASES.put("fitness centre", PointOfInterestType.GYM);
        ALIASES.put("workout", PointOfInterestType.GYM);
        ALIASES.put("health club", PointOfInterestType.GYM);
        
        // Cemetery
        ALIASES.put("cemetery", PointOfInterestType.CEMETERY);
        ALIASES.put("cemeteries", PointOfInterestType.CEMETERY);
        ALIASES.put("graveyard", PointOfInterestType.GRAVE_YARD);
        ALIASES.put("grave yard", PointOfInterestType.GRAVE_YARD);
        ALIASES.put("burial ground", PointOfInterestType.CEMETERY);
        ALIASES.put("memorial park", PointOfInterestType.CEMETERY);
        
        // Place of Worship
        ALIASES.put("church", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("churches", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("mosque", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("temple", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("synagogue", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("chapel", PointOfInterestType.PLACE_OF_WORSHIP);
        ALIASES.put("place of worship", PointOfInterestType.PLACE_OF_WORSHIP);
        
        // Library
        ALIASES.put("library", PointOfInterestType.LIBRARY);
        ALIASES.put("libraries", PointOfInterestType.LIBRARY);
        ALIASES.put("public library", PointOfInterestType.LIBRARY);
        
        // Post Office
        ALIASES.put("post office", PointOfInterestType.POST_OFFICE);
        ALIASES.put("postal", PointOfInterestType.POST_OFFICE);
        ALIASES.put("usps", PointOfInterestType.POST_OFFICE);
        ALIASES.put("mail", PointOfInterestType.POST_OFFICE);
        
        // Embassy
        ALIASES.put("embassy", PointOfInterestType.EMBASSY);
        ALIASES.put("embassies", PointOfInterestType.EMBASSY);
        ALIASES.put("consulate", PointOfInterestType.EMBASSY);
        
        // Government
        ALIASES.put("government", PointOfInterestType.GOVERNMENT);
        ALIASES.put("government office", PointOfInterestType.GOVERNMENT);
        ALIASES.put("city hall", PointOfInterestType.GOVERNMENT);
        ALIASES.put("town hall", PointOfInterestType.GOVERNMENT);
        ALIASES.put("dmv", PointOfInterestType.GOVERNMENT);
        
        // Prison
        ALIASES.put("prison", PointOfInterestType.PRISON);
        ALIASES.put("jail", PointOfInterestType.PRISON);
        ALIASES.put("correctional", PointOfInterestType.PRISON);
        
        // Surveillance
        ALIASES.put("camera", PointOfInterestType.SURVEILLANCE);
        ALIASES.put("cameras", PointOfInterestType.SURVEILLANCE);
        ALIASES.put("surveillance", PointOfInterestType.SURVEILLANCE);
        ALIASES.put("cctv", PointOfInterestType.SURVEILLANCE);
        
        // Communication Towers
        ALIASES.put("comm tower", PointOfInterestType.COMM_TOWER);
        ALIASES.put("communication tower", PointOfInterestType.COMM_TOWER);
        ALIASES.put("radio tower", PointOfInterestType.COMM_TOWER);
        ALIASES.put("cell tower", PointOfInterestType.CELL_TOWER);
        ALIASES.put("cell phone tower", PointOfInterestType.CELL_TOWER);
        ALIASES.put("cellular tower", PointOfInterestType.CELL_TOWER);
        
        // Power Station
        ALIASES.put("power station", PointOfInterestType.POWER_STATION);
        ALIASES.put("power plant", PointOfInterestType.POWER_STATION);
        ALIASES.put("electric", PointOfInterestType.POWER_STATION);
        
        // Water Tower
        ALIASES.put("water tower", PointOfInterestType.WATER_TOWER);
    }
    
    /**
     * Result of category matching.
     */
    public static class MatchResult {
        private final PointOfInterestType category;
        private final boolean isNearbyQuery;
        private final String originalQuery;
        private final String cleanedQuery;
        
        public MatchResult(PointOfInterestType category, boolean isNearbyQuery, 
                          String originalQuery, String cleanedQuery) {
            this.category = category;
            this.isNearbyQuery = isNearbyQuery;
            this.originalQuery = originalQuery;
            this.cleanedQuery = cleanedQuery;
        }
        
        public PointOfInterestType getCategory() {
            return category;
        }
        
        public boolean isNearbyQuery() {
            return isNearbyQuery;
        }
        
        public String getOriginalQuery() {
            return originalQuery;
        }
        
        public String getCleanedQuery() {
            return cleanedQuery;
        }
        
        public boolean hasMatch() {
            return category != null;
        }
    }
    
    /**
     * Detect if a query is asking for a POI category search.
     * 
     * @param query The user's search query
     * @return MatchResult with detected category (if any) and metadata
     */
    public static MatchResult detectCategory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new MatchResult(null, false, query, query);
        }
        
        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        
        // Check for "near me/nearby" suffix
        boolean isNearbyQuery = false;
        String cleanedQuery = normalizedQuery;
        
        Matcher nearbyMatcher = NEARBY_PATTERN.matcher(normalizedQuery);
        if (nearbyMatcher.find()) {
            isNearbyQuery = true;
            cleanedQuery = normalizedQuery.substring(0, nearbyMatcher.start()).trim();
        }
        
        // Try exact match first (most accurate)
        PointOfInterestType exactMatch = ALIASES.get(cleanedQuery);
        if (exactMatch != null) {
            return new MatchResult(exactMatch, isNearbyQuery, query, cleanedQuery);
        }
        
        // Try partial/contains match for multi-word aliases
        for (Map.Entry<String, PointOfInterestType> entry : ALIASES.entrySet()) {
            String alias = entry.getKey();
            // Check if the query contains the alias as a word boundary match
            if (cleanedQuery.equals(alias) || 
                cleanedQuery.startsWith(alias + " ") ||
                cleanedQuery.endsWith(" " + alias) ||
                cleanedQuery.contains(" " + alias + " ")) {
                return new MatchResult(entry.getValue(), isNearbyQuery, query, cleanedQuery);
            }
        }
        
        // Try fuzzy matching for typos
        PointOfInterestType fuzzyMatch = findFuzzyMatch(cleanedQuery);
        if (fuzzyMatch != null) {
            return new MatchResult(fuzzyMatch, isNearbyQuery, query, cleanedQuery);
        }
        
        // If it's a nearby query but no category found, still mark it
        // This allows the UI to show a helpful message
        return new MatchResult(null, isNearbyQuery, query, cleanedQuery);
    }
    
    /**
     * Check if a query is asking for nearby results.
     */
    public static boolean isNearbyQuery(String query) {
        if (query == null) return false;
        return NEARBY_PATTERN.matcher(query.toLowerCase(Locale.ROOT)).find();
    }
    
    /**
     * Remove the "near me/nearby" suffix from a query.
     */
    public static String extractSearchTerms(String query) {
        if (query == null) return "";
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        Matcher matcher = NEARBY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return normalized.substring(0, matcher.start()).trim();
        }
        return normalized;
    }
    
    /**
     * Get all matching categories for a query (for potential multiple matches).
     */
    public static List<PointOfInterestType> detectCategories(String query) {
        List<PointOfInterestType> matches = new ArrayList<>();
        MatchResult result = detectCategory(query);
        if (result.hasMatch()) {
            matches.add(result.getCategory());
        }
        return matches;
    }
    
    /**
     * Find a fuzzy match using Levenshtein distance.
     */
    private static PointOfInterestType findFuzzyMatch(String query) {
        if (query.length() < 3) {
            return null; // Don't fuzzy match very short queries
        }
        
        PointOfInterestType bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        
        for (Map.Entry<String, PointOfInterestType> entry : ALIASES.entrySet()) {
            String alias = entry.getKey();
            
            // Calculate maximum allowed distance based on word length
            int maxDistance = Math.max(1, (int) (alias.length() * MAX_DISTANCE_RATIO));
            
            int distance = levenshteinDistance(query, alias);
            
            if (distance <= maxDistance && distance < bestDistance) {
                bestDistance = distance;
                bestMatch = entry.getValue();
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Calculate Levenshtein distance between two strings.
     */
    private static int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * Get the display name for a category (for UI messages).
     */
    public static String getCategoryDisplayName(PointOfInterestType type) {
        if (type == null) return "";
        // Convert enum name to readable format: GAS_STATION -> Gas Station
        String name = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
                if (c == ' ') {
                    capitalizeNext = true;
                }
            }
        }
        return result.toString();
    }
}

