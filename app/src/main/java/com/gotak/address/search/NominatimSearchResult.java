package com.gotak.address.search;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data model for Nominatim API search results.
 * Maps to the JSON response from https://nominatim.openstreetmap.org/search
 */
public class NominatimSearchResult {
    private final long placeId;
    private final double latitude;
    private final double longitude;
    private final String displayName;
    private final String name;
    private final String type;
    private final String osmType;
    private final long osmId;

    public NominatimSearchResult(long placeId, double latitude, double longitude,
                                  String displayName, String name, String type,
                                  String osmType, long osmId) {
        this.placeId = placeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.displayName = displayName;
        this.name = name;
        this.type = type;
        this.osmType = osmType;
        this.osmId = osmId;
    }

    /**
     * Parse a NominatimSearchResult from a JSON object.
     */
    public static NominatimSearchResult fromJson(JSONObject json) throws JSONException {
        long placeId = json.optLong("place_id", 0);
        
        // Lat/lon come as strings in the API response
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            latitude = Double.parseDouble(json.optString("lat", "0"));
            longitude = Double.parseDouble(json.optString("lon", "0"));
        } catch (NumberFormatException e) {
            // Keep defaults
        }
        
        String displayName = json.optString("display_name", "");
        String name = json.optString("name", null);
        String type = json.optString("type", null);
        String osmType = json.optString("osm_type", null);
        long osmId = json.optLong("osm_id", 0);

        return new NominatimSearchResult(placeId, latitude, longitude,
                displayName, name, type, osmType, osmId);
    }

    public long getPlaceId() {
        return placeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the short name for this result.
     * Falls back to first part of display name if name is null.
     */
    public String getName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        // Fallback: use first part of display name
        if (displayName != null && displayName.contains(",")) {
            return displayName.substring(0, displayName.indexOf(",")).trim();
        }
        return displayName;
    }

    public String getType() {
        return type;
    }

    public String getOsmType() {
        return osmType;
    }

    public long getOsmId() {
        return osmId;
    }

    /**
     * Get an appropriate zoom level based on the result type.
     * Lower numbers = more zoomed out. ATAK zoom range is roughly 1-21.
     */
    public double getZoomLevel() {
        if (type == null) {
            return 7.0;
        }
        switch (type) {
            case "country":
                return 3.0;
            case "state":
            case "region":
            case "county":
                return 4.0;
            case "city":
            case "town":
                return 6.0;
            default:
                // Villages, buildings, shops, places, etc.
                return 7.0;
        }
    }

    /**
     * Convert to JSON for persistence.
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("place_id", placeId);
        json.put("lat", String.valueOf(latitude));
        json.put("lon", String.valueOf(longitude));
        json.put("display_name", displayName);
        json.put("name", name);
        json.put("type", type);
        json.put("osm_type", osmType);
        json.put("osm_id", osmId);
        return json;
    }

    @Override
    public String toString() {
        return "NominatimSearchResult{" +
                "placeId=" + placeId +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", name='" + getName() + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

