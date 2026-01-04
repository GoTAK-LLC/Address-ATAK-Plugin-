package com.gotak.address.search;

import com.gotak.address.plugin.R;

/**
 * Categorizes Nominatim results by type for icon and styling purposes.
 */
public enum LocationType {
    CITY(R.drawable.ic_location_city),
    TOWN(R.drawable.ic_location_city),
    VILLAGE(R.drawable.ic_location_city),
    COUNTRY(R.drawable.ic_location_world),
    STATE(R.drawable.ic_location_world),
    REGION(R.drawable.ic_location_world),
    BUILDING(R.drawable.ic_location_pin),
    ADDRESS(R.drawable.ic_location_pin),
    OTHER(R.drawable.ic_location_map);

    private final int iconRes;

    LocationType(int iconRes) {
        this.iconRes = iconRes;
    }

    public int getIconRes() {
        return iconRes;
    }

    /**
     * Determine the LocationType from a NominatimSearchResult.
     */
    public static LocationType fromResult(NominatimSearchResult result) {
        String type = result.getType();
        if (type == null) {
            return OTHER;
        }
        
        String typeLower = type.toLowerCase();
        
        switch (typeLower) {
            // Cities and urban areas
            case "city":
                return CITY;
            case "town":
                return TOWN;
            case "village":
            case "hamlet":
            case "suburb":
            case "neighbourhood":
            case "quarter":
                return VILLAGE;
            
            // Countries and regions
            case "country":
                return COUNTRY;
            case "state":
            case "province":
                return STATE;
            case "county":
            case "region":
            case "municipality":
            case "district":
            case "administrative":
                return REGION;
            
            // Buildings and addresses
            case "building":
            case "house":
            case "residential":
            case "apartments":
            case "hotel":
            case "retail":
            case "commercial":
            case "industrial":
            case "warehouse":
            case "church":
            case "school":
            case "university":
            case "hospital":
            case "stadium":
            case "museum":
            case "library":
            case "theatre":
            case "cinema":
            case "restaurant":
            case "cafe":
            case "bar":
            case "pub":
            case "bank":
            case "pharmacy":
            case "supermarket":
            case "shop":
                return BUILDING;
            
            case "address":
            case "street":
            case "road":
            case "path":
            case "highway":
                return ADDRESS;
            
            default:
                return OTHER;
        }
    }
}

