package com.gotak.address.search.nearby;

import org.json.JSONObject;

/**
 * Data model representing a Point of Interest search result from the Overpass API.
 */
public class OverpassSearchResult {

    private final long osmId;
    private final String osmType;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double distanceMeters;
    private final PointOfInterestType poiType;
    private final String address;
    private final JSONObject tags;

    public OverpassSearchResult(long osmId, String osmType, String name, 
                                 double latitude, double longitude, 
                                 double distanceMeters, PointOfInterestType poiType,
                                 String address, JSONObject tags) {
        this.osmId = osmId;
        this.osmType = osmType;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.poiType = poiType;
        this.address = address;
        this.tags = tags;
    }

    public long getOsmId() {
        return osmId;
    }

    public String getOsmType() {
        return osmType;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public PointOfInterestType getPoiType() {
        return poiType;
    }

    public String getAddress() {
        return address;
    }

    public JSONObject getTags() {
        return tags;
    }

    /**
     * Get a formatted distance string (e.g., "1.2 km" or "350 m").
     */
    public String getFormattedDistance() {
        if (distanceMeters >= 1000) {
            return String.format("%.1f km", distanceMeters / 1000.0);
        } else {
            return String.format("%.0f m", distanceMeters);
        }
    }

    /**
     * Get a display name that includes the POI type if the name is generic.
     */
    public String getDisplayName() {
        if (name == null || name.isEmpty()) {
            return poiType != null ? poiType.getOsmValue().replace("_", " ") : "Unknown";
        }
        return name;
    }

    /**
     * Generate a unique ID for this result.
     */
    public String getUniqueId() {
        return osmType + "-" + osmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverpassSearchResult that = (OverpassSearchResult) o;
        return osmId == that.osmId && osmType.equals(that.osmType);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(osmId) * 31 + osmType.hashCode();
    }
}

