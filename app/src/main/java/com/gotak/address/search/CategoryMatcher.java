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
 * - "gas arkansas" (state-specific POI search)
 * - "walmart texas" (state-specific name search)
 * - "hospital in virginia" (location with preposition)
 * 
 * Uses keyword aliases and fuzzy matching to map user input to POI categories.
 */
public class CategoryMatcher {
    
    // Patterns to detect "near me", "nearby", "near" suffixes
    private static final Pattern NEARBY_PATTERN = Pattern.compile(
            "\\s*(near\\s*me|nearby|near\\s*by|close\\s*by|around\\s*me|near)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for "in [location]" suffix (e.g., "gas stations in arkansas")
    private static final Pattern IN_LOCATION_PATTERN = Pattern.compile(
            "\\s+in\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    
    // Maximum Levenshtein distance for fuzzy matching (relative to word length)
    private static final double MAX_DISTANCE_RATIO = 0.3; // 30% of word length
    
    // Alias map: lowercase keyword -> POI type
    private static final Map<String, PointOfInterestType> ALIASES = new HashMap<>();
    
    // US State names and abbreviations -> database ID (lowercase, hyphenated)
    private static final Map<String, String> STATE_NAMES = new HashMap<>();
    
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
        
        // US State names and abbreviations -> database ID
        // Full names (lowercase)
        STATE_NAMES.put("alabama", "alabama");
        STATE_NAMES.put("alaska", "alaska");
        STATE_NAMES.put("arizona", "arizona");
        STATE_NAMES.put("arkansas", "arkansas");
        STATE_NAMES.put("california", "california");
        STATE_NAMES.put("colorado", "colorado");
        STATE_NAMES.put("connecticut", "connecticut");
        STATE_NAMES.put("delaware", "delaware");
        STATE_NAMES.put("florida", "florida");
        STATE_NAMES.put("georgia", "georgia");
        STATE_NAMES.put("hawaii", "hawaii");
        STATE_NAMES.put("idaho", "idaho");
        STATE_NAMES.put("illinois", "illinois");
        STATE_NAMES.put("indiana", "indiana");
        STATE_NAMES.put("iowa", "iowa");
        STATE_NAMES.put("kansas", "kansas");
        STATE_NAMES.put("kentucky", "kentucky");
        STATE_NAMES.put("louisiana", "louisiana");
        STATE_NAMES.put("maine", "maine");
        STATE_NAMES.put("maryland", "maryland");
        STATE_NAMES.put("massachusetts", "massachusetts");
        STATE_NAMES.put("michigan", "michigan");
        STATE_NAMES.put("minnesota", "minnesota");
        STATE_NAMES.put("mississippi", "mississippi");
        STATE_NAMES.put("missouri", "missouri");
        STATE_NAMES.put("montana", "montana");
        STATE_NAMES.put("nebraska", "nebraska");
        STATE_NAMES.put("nevada", "nevada");
        STATE_NAMES.put("new hampshire", "new-hampshire");
        STATE_NAMES.put("new jersey", "new-jersey");
        STATE_NAMES.put("new mexico", "new-mexico");
        STATE_NAMES.put("new york", "new-york");
        STATE_NAMES.put("north carolina", "north-carolina");
        STATE_NAMES.put("north dakota", "north-dakota");
        STATE_NAMES.put("ohio", "ohio");
        STATE_NAMES.put("oklahoma", "oklahoma");
        STATE_NAMES.put("oregon", "oregon");
        STATE_NAMES.put("pennsylvania", "pennsylvania");
        STATE_NAMES.put("rhode island", "rhode-island");
        STATE_NAMES.put("south carolina", "south-carolina");
        STATE_NAMES.put("south dakota", "south-dakota");
        STATE_NAMES.put("tennessee", "tennessee");
        STATE_NAMES.put("texas", "texas");
        STATE_NAMES.put("utah", "utah");
        STATE_NAMES.put("vermont", "vermont");
        STATE_NAMES.put("virginia", "virginia");
        STATE_NAMES.put("washington", "washington");
        STATE_NAMES.put("west virginia", "west-virginia");
        STATE_NAMES.put("wisconsin", "wisconsin");
        STATE_NAMES.put("wyoming", "wyoming");
        STATE_NAMES.put("district of columbia", "district-of-columbia");
        STATE_NAMES.put("dc", "district-of-columbia");
        STATE_NAMES.put("washington dc", "district-of-columbia");
        STATE_NAMES.put("washington d.c.", "district-of-columbia");
        
        // Abbreviations
        STATE_NAMES.put("al", "alabama");
        STATE_NAMES.put("ak", "alaska");
        STATE_NAMES.put("az", "arizona");
        STATE_NAMES.put("ar", "arkansas");
        STATE_NAMES.put("ca", "california");
        STATE_NAMES.put("co", "colorado");
        STATE_NAMES.put("ct", "connecticut");
        STATE_NAMES.put("de", "delaware");
        STATE_NAMES.put("fl", "florida");
        STATE_NAMES.put("ga", "georgia");
        STATE_NAMES.put("hi", "hawaii");
        STATE_NAMES.put("id", "idaho");
        STATE_NAMES.put("il", "illinois");
        STATE_NAMES.put("in", "indiana");
        STATE_NAMES.put("ia", "iowa");
        STATE_NAMES.put("ks", "kansas");
        STATE_NAMES.put("ky", "kentucky");
        STATE_NAMES.put("la", "louisiana");
        STATE_NAMES.put("me", "maine");
        STATE_NAMES.put("md", "maryland");
        STATE_NAMES.put("ma", "massachusetts");
        STATE_NAMES.put("mi", "michigan");
        STATE_NAMES.put("mn", "minnesota");
        STATE_NAMES.put("ms", "mississippi");
        STATE_NAMES.put("mo", "missouri");
        STATE_NAMES.put("mt", "montana");
        STATE_NAMES.put("ne", "nebraska");
        STATE_NAMES.put("nv", "nevada");
        STATE_NAMES.put("nh", "new-hampshire");
        STATE_NAMES.put("nj", "new-jersey");
        STATE_NAMES.put("nm", "new-mexico");
        STATE_NAMES.put("ny", "new-york");
        STATE_NAMES.put("nc", "north-carolina");
        STATE_NAMES.put("nd", "north-dakota");
        STATE_NAMES.put("oh", "ohio");
        STATE_NAMES.put("ok", "oklahoma");
        STATE_NAMES.put("or", "oregon");
        STATE_NAMES.put("pa", "pennsylvania");
        STATE_NAMES.put("ri", "rhode-island");
        STATE_NAMES.put("sc", "south-carolina");
        STATE_NAMES.put("sd", "south-dakota");
        STATE_NAMES.put("tn", "tennessee");
        STATE_NAMES.put("tx", "texas");
        STATE_NAMES.put("ut", "utah");
        STATE_NAMES.put("vt", "vermont");
        STATE_NAMES.put("va", "virginia");
        STATE_NAMES.put("wa", "washington");
        STATE_NAMES.put("wv", "west-virginia");
        STATE_NAMES.put("wi", "wisconsin");
        STATE_NAMES.put("wy", "wyoming");
    }
    
    /**
     * Result of category matching.
     */
    public static class MatchResult {
        private final PointOfInterestType category;
        private final boolean isNearbyQuery;
        private final String originalQuery;
        private final String cleanedQuery;
        private final String stateId;      // Database ID of detected state (e.g., "arkansas")
        private final String searchTerm;   // The search term without location (e.g., "gas", "walmart")
        
        public MatchResult(PointOfInterestType category, boolean isNearbyQuery, 
                          String originalQuery, String cleanedQuery) {
            this(category, isNearbyQuery, originalQuery, cleanedQuery, null, cleanedQuery);
        }
        
        public MatchResult(PointOfInterestType category, boolean isNearbyQuery, 
                          String originalQuery, String cleanedQuery, 
                          String stateId, String searchTerm) {
            this.category = category;
            this.isNearbyQuery = isNearbyQuery;
            this.originalQuery = originalQuery;
            this.cleanedQuery = cleanedQuery;
            this.stateId = stateId;
            this.searchTerm = searchTerm;
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
        
        /**
         * Get the state database ID if a location was detected.
         * @return State database ID (e.g., "arkansas", "new-york") or null
         */
        public String getStateId() {
            return stateId;
        }
        
        /**
         * Get the search term without the location suffix.
         * For "gas arkansas" this would return "gas".
         */
        public String getSearchTerm() {
            return searchTerm;
        }
        
        /**
         * Check if this is a location-specific query.
         */
        public boolean hasLocation() {
            return stateId != null;
        }
        
        public boolean hasMatch() {
            return category != null;
        }
    }
    
    /**
     * Detect if a query is asking for a POI category search.
     * 
     * Handles patterns like:
     * - "gas station near me" (nearby query)
     * - "gas arkansas" (location-specific POI search)
     * - "walmart texas" (location-specific name search)
     * - "hospitals in virginia" (explicit location)
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
        
        // Check for location patterns (e.g., "gas arkansas", "walmart in texas")
        LocationParseResult locationResult = parseLocation(cleanedQuery);
        if (locationResult != null) {
            String searchTerm = locationResult.searchTerm;
            String stateId = locationResult.stateId;
            
            // Try to match the search term to a category
            PointOfInterestType category = matchCategoryFromTerm(searchTerm);
            
            // Return result with location info (even if no category match - it might be a name search like "walmart")
            return new MatchResult(category, isNearbyQuery, query, cleanedQuery, stateId, searchTerm);
        }
        
        // Standard category matching (no location specified)
        
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
     * Result of parsing a location from a query.
     */
    private static class LocationParseResult {
        final String searchTerm;
        final String stateId;
        
        LocationParseResult(String searchTerm, String stateId) {
            this.searchTerm = searchTerm;
            this.stateId = stateId;
        }
    }
    
    /**
     * Parse a location (state) from the query.
     * Handles patterns like:
     * - "gas arkansas" -> searchTerm="gas", stateId="arkansas"
     * - "walmart in texas" -> searchTerm="walmart", stateId="texas"
     * - "hospitals in new york" -> searchTerm="hospitals", stateId="new-york"
     */
    private static LocationParseResult parseLocation(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        
        String searchTerm = query;
        String locationPart = null;
        
        // Check for "in [location]" pattern first
        Matcher inMatcher = IN_LOCATION_PATTERN.matcher(query);
        if (inMatcher.find()) {
            searchTerm = query.substring(0, inMatcher.start()).trim();
            locationPart = inMatcher.group(1).trim();
            
            String stateId = STATE_NAMES.get(locationPart);
            if (stateId != null && !searchTerm.isEmpty()) {
                return new LocationParseResult(searchTerm, stateId);
            }
        }
        
        // Try matching state names at the end of the query
        // Start with longer state names (e.g., "new york" before "york")
        String[] words = query.split("\\s+");
        if (words.length < 2) {
            return null; // Need at least a search term and a location
        }
        
        // Try 3-word state names (e.g., "district of columbia")
        if (words.length >= 4) {
            String threeWordLocation = words[words.length - 3] + " " + 
                                       words[words.length - 2] + " " + 
                                       words[words.length - 1];
            String stateId = STATE_NAMES.get(threeWordLocation);
            if (stateId != null) {
                searchTerm = joinWords(words, 0, words.length - 3);
                if (!searchTerm.isEmpty()) {
                    return new LocationParseResult(searchTerm, stateId);
                }
            }
        }
        
        // Try 2-word state names (e.g., "new york", "north carolina")
        if (words.length >= 3) {
            String twoWordLocation = words[words.length - 2] + " " + words[words.length - 1];
            String stateId = STATE_NAMES.get(twoWordLocation);
            if (stateId != null) {
                searchTerm = joinWords(words, 0, words.length - 2);
                if (!searchTerm.isEmpty()) {
                    return new LocationParseResult(searchTerm, stateId);
                }
            }
        }
        
        // Try 1-word state names (e.g., "arkansas", "texas", "va")
        String oneWordLocation = words[words.length - 1];
        String stateId = STATE_NAMES.get(oneWordLocation);
        if (stateId != null) {
            searchTerm = joinWords(words, 0, words.length - 1);
            if (!searchTerm.isEmpty()) {
                return new LocationParseResult(searchTerm, stateId);
            }
        }
        
        return null;
    }
    
    /**
     * Join words from start to end index.
     */
    private static String joinWords(String[] words, int start, int end) {
        if (start >= end || start < 0 || end > words.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString();
    }
    
    /**
     * Try to match a search term to a POI category.
     */
    private static PointOfInterestType matchCategoryFromTerm(String term) {
        if (term == null || term.isEmpty()) {
            return null;
        }
        
        // Try exact match
        PointOfInterestType exact = ALIASES.get(term);
        if (exact != null) {
            return exact;
        }
        
        // Try partial match
        for (Map.Entry<String, PointOfInterestType> entry : ALIASES.entrySet()) {
            String alias = entry.getKey();
            if (term.equals(alias) || 
                term.startsWith(alias + " ") ||
                term.endsWith(" " + alias) ||
                term.contains(" " + alias + " ")) {
                return entry.getValue();
            }
        }
        
        // Try fuzzy match
        return findFuzzyMatch(term);
    }
    
    /**
     * Check if a state ID corresponds to a downloaded database.
     * This is a static helper - actual database checking should be done by the caller.
     */
    public static String normalizeStateId(String input) {
        if (input == null) return null;
        String stateId = STATE_NAMES.get(input.toLowerCase(Locale.ROOT).trim());
        return stateId;
    }
    
    /**
     * Get the display name for a state ID.
     */
    public static String getStateDisplayName(String stateId) {
        if (stateId == null) return "";
        // Convert database ID to display name: "new-york" -> "New York"
        return stateId.replace('-', ' ')
                .replaceAll("(^|\\s)(\\w)", new java.util.function.Function<java.util.regex.MatchResult, String>() {
                    @Override
                    public String apply(java.util.regex.MatchResult m) {
                        return m.group().toUpperCase();
                    }
                }.toString());
    }
    
    /**
     * Get a nicely formatted state display name.
     */
    public static String formatStateDisplayName(String stateId) {
        if (stateId == null) return "";
        String[] parts = stateId.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
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

