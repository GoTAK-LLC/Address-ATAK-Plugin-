package com.gotak.address.search.nearby;

import com.gotak.address.plugin.R;

/**
 * Enum defining POI categories with their corresponding OpenStreetMap Overpass API tags.
 * Each type has:
 * - OSM key-value pair for querying
 * - String resource for display name
 * - CoT type code for fallback MIL-STD-2525 rendering
 * - Icon name for custom iconset (from Nearby plugin's iconset)
 * 
 * Fallback CoT types use standard MIL-STD-2525 symbols built into ATAK:
 * - a-n-G = Neutral Ground (generic)
 * - a-n-G-I-* = Neutral Ground Installation types
 */
public enum PointOfInterestType {
    // Medical/Emergency
    HOSPITAL("amenity", "hospital", R.string.poi_hospital, "a-n-G-I-H", "hospital.png"),
    PHARMACY("amenity", "pharmacy", R.string.poi_pharmacy, "a-n-G-I-H", "hospital.png"),
    
    // Emergency Services
    FIRE_STATION("amenity", "fire_station", R.string.poi_fire_station, "a-n-G-I-FF", "firebrigade.png"),
    POLICE_STATION("amenity", "police", R.string.poi_police_station, "a-n-G-I-LP", "police.png"),
    
    // Transportation - Aviation
    AIRPORT("aeroway", "aerodrome", R.string.poi_airport, "a-n-G-I-BA", "airport.png"),
    HELIPORT("aeroway", "helipad", R.string.poi_heliport, "a-n-G-I-BH", "helipad.png"),
    
    // Transportation - Ground/Water
    RAILWAY_STATION("railway", "station", R.string.poi_railway_station, "a-n-G-I-USR", "railway_station.png"),
    FERRY_TERMINAL("amenity", "ferry_terminal", R.string.poi_ferry_terminal, "a-n-G-I-USUSP", "ferry.png"),
    PARKING("amenity", "parking", R.string.poi_parking, "a-n-G", "parking.png"),
    
    // Fuel/Services
    GAS_STATION("amenity", "fuel", R.string.poi_gas_station, "a-n-G-I-RP", "fuel.png"),
    
    // Finance
    BANK("amenity", "bank", R.string.poi_bank, "a-n-G", "bank.png"),
    ATM("amenity", "atm", R.string.poi_atm, "a-n-G", "bank.png"),
    
    // Education
    SCHOOL("amenity", "school", R.string.poi_school, "a-n-G", "education.png"),
    
    // Food/Lodging
    RESTAURANT("amenity", "restaurant", R.string.poi_restaurant, "a-n-G", "restaurant.png"),
    HOTEL("tourism", "hotel", R.string.poi_hotel, "a-n-G", "hotel.png"),
    CAFE("amenity", "cafe", R.string.poi_cafe, "a-n-G", "cafe.png"),
    FAST_FOOD("amenity", "fast_food", R.string.poi_fast_food, "a-n-G", "restaurant.png"),
    BAR("amenity", "bar", R.string.poi_bar, "a-n-G", "bar.png"),
    PUB("amenity", "pub", R.string.poi_pub, "a-n-G", "bar.png"),
    
    // Shopping/Groceries
    SUPERMARKET("shop", "supermarket", R.string.poi_supermarket, "a-n-G", "supermarket.png"),
    CONVENIENCE_STORE("shop", "convenience", R.string.poi_convenience_store, "a-n-G", "convenience.png"),
    SHOPPING_MALL("shop", "mall", R.string.poi_shopping_mall, "a-n-G", "mall.png"),
    HARDWARE_STORE("shop", "hardware", R.string.poi_hardware_store, "a-n-G", "hardware.png"),
    
    // Health/Medical
    DENTIST("amenity", "dentist", R.string.poi_dentist, "a-n-G-I-H", "hospital.png"),
    DOCTOR("amenity", "doctors", R.string.poi_doctor, "a-n-G-I-H", "hospital.png"),
    CLINIC("amenity", "clinic", R.string.poi_clinic, "a-n-G-I-H", "hospital.png"),
    VETERINARIAN("amenity", "veterinary", R.string.poi_veterinarian, "a-n-G", "veterinary.png"),
    
    // Services
    CAR_WASH("amenity", "car_wash", R.string.poi_car_wash, "a-n-G", "car_wash.png"),
    LAUNDRY("shop", "laundry", R.string.poi_laundry, "a-n-G", "laundry.png"),
    HAIR_SALON("shop", "hairdresser", R.string.poi_hair_salon, "a-n-G", "salon.png"),
    
    // Entertainment/Recreation
    CINEMA("amenity", "cinema", R.string.poi_cinema, "a-n-G", "cinema.png"),
    GYM("leisure", "fitness_centre", R.string.poi_gym, "a-n-G", "gym.png"),
    
    // Cemetery/Memorial
    CEMETERY("landuse", "cemetery", R.string.poi_cemetery, "a-n-G", "cemetery.png"),
    GRAVE_YARD("amenity", "grave_yard", R.string.poi_grave_yard, "a-n-G", "cemetery.png"),
    
    // Surveillance/Security - uses generic ground with custom icon
    SURVEILLANCE("man_made", "surveillance", R.string.poi_surveillance, "a-n-G", "surveillance.png"),
    
    // Government/Diplomatic
    EMBASSY("amenity", "embassy", R.string.poi_embassy, "a-n-G", "embassy.png"),
    GOVERNMENT("office", "government", R.string.poi_government, "a-n-G", "city.png"),
    PRISON("amenity", "prison", R.string.poi_prison, "a-n-G", "prison.png"),
    
    // Communications/Infrastructure - uses generic ground with custom icon
    COMM_TOWER("man_made", "tower", R.string.poi_comm_tower, "a-n-G", "tower.png"),
    CELL_TOWER("man_made", "mast", R.string.poi_cell_tower, "a-n-G", "tower.png"),
    POWER_STATION("power", "plant", R.string.poi_power_station, "a-n-G-I-UP", "power.png"),
    WATER_TOWER("man_made", "water_tower", R.string.poi_water_tower, "a-n-G", "water_tower.png"),
    
    // Utilities
    POST_OFFICE("amenity", "post_office", R.string.poi_post_office, "a-n-G", "post_office.png"),
    
    // Community
    PLACE_OF_WORSHIP("amenity", "place_of_worship", R.string.poi_place_of_worship, "a-n-G", "worship.png"),
    LIBRARY("amenity", "library", R.string.poi_library, "a-n-G", "library.png");

    private final String osmKey;
    private final String osmValue;
    private final int stringResId;
    private final String cotType;
    private final String iconName;

    PointOfInterestType(String osmKey, String osmValue, int stringResId, String cotType, String iconName) {
        this.osmKey = osmKey;
        this.osmValue = osmValue;
        this.stringResId = stringResId;
        this.cotType = cotType;
        this.iconName = iconName;
    }

    /**
     * Get the OSM key for this POI type (e.g., "amenity", "aeroway").
     */
    public String getOsmKey() {
        return osmKey;
    }

    /**
     * Get the OSM value for this POI type (e.g., "hospital", "fuel").
     */
    public String getOsmValue() {
        return osmValue;
    }

    /**
     * Get the string resource ID for the display name.
     */
    public int getStringResId() {
        return stringResId;
    }

    /**
     * Get the CoT type code for marker creation - used as fallback for MIL-STD-2525 rendering.
     */
    public String getCotType() {
        return cotType;
    }

    /**
     * Get the icon filename for custom iconset rendering.
     */
    public String getIconName() {
        return iconName;
    }

    /**
     * Generate an Overpass query fragment for this POI type.
     * Example: node["amenity"="hospital"](around:5000,lat,lon);
     */
    public String toOverpassQueryFragment(double lat, double lon, int radiusMeters) {
        return String.format(
            "node[\"%s\"=\"%s\"](around:%d,%f,%f);\n" +
            "way[\"%s\"=\"%s\"](around:%d,%f,%f);",
            osmKey, osmValue, radiusMeters, lat, lon,
            osmKey, osmValue, radiusMeters, lat, lon
        );
    }
}
