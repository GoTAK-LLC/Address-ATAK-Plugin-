package com.gotak.address.search.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Data model representing a saved map view.
 * Captures camera state for both 2D and 3D modes.
 */
public class SavedView {
    
    private String id;
    private String name;
    private long createdAt;
    
    // Location
    private double latitude;
    private double longitude;
    
    // Camera state
    private double zoom;           // Map scale (stored raw from ATAK's getMapScale)
    private double altitude;       // 3D altitude in meters (HAE)
    private double tilt;           // 3D tilt/pitch in degrees (0 = looking down, 90 = horizon)
    private double rotation;       // Heading/azimuth in degrees (0 = north)
    private boolean is3DMode;      // Whether view was captured in 3D mode
    
    // Display
    private String thumbnailBase64; // Base64-encoded PNG thumbnail
    private String geocodedAddress; // Reverse-geocoded address for display
    
    /**
     * Create a new SavedView with auto-generated ID.
     */
    public SavedView() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Create a SavedView from captured map state.
     */
    public static SavedView capture(String name, double lat, double lon, double zoom,
                                     double altitude, double tilt, double rotation,
                                     boolean is3D, Bitmap thumbnail, String address) {
        SavedView view = new SavedView();
        view.name = name;
        view.latitude = lat;
        view.longitude = lon;
        view.zoom = zoom;
        view.altitude = altitude;
        view.tilt = tilt;
        view.rotation = rotation;
        view.is3DMode = is3D;
        view.geocodedAddress = address;
        
        if (thumbnail != null) {
            view.setThumbnail(thumbnail);
        }
        
        return view;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getZoom() { return zoom; }
    public double getAltitude() { return altitude; }
    public double getTilt() { return tilt; }
    public double getRotation() { return rotation; }
    public boolean is3DMode() { return is3DMode; }
    public String getGeocodedAddress() { return geocodedAddress; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setGeocodedAddress(String address) { this.geocodedAddress = address; }
    
    /**
     * Set thumbnail from Bitmap (encodes to Base64).
     */
    public void setThumbnail(Bitmap bitmap) {
        if (bitmap == null) {
            this.thumbnailBase64 = null;
            return;
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Use JPEG with high quality for crisp thumbnail display
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        this.thumbnailBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    
    /**
     * Get thumbnail as Bitmap (decodes from Base64).
     */
    public Bitmap getThumbnail() {
        if (thumbnailBase64 == null || thumbnailBase64.isEmpty()) {
            return null;
        }
        
        try {
            byte[] bytes = Base64.decode(thumbnailBase64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if this view has a thumbnail.
     */
    public boolean hasThumbnail() {
        return thumbnailBase64 != null && !thumbnailBase64.isEmpty();
    }
    
    /**
     * Get display subtitle (mode + address).
     */
    public String getSubtitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(is3DMode ? "3D" : "2D");
        
        if (geocodedAddress != null && !geocodedAddress.isEmpty()) {
            sb.append(" • ").append(geocodedAddress);
        }
        
        return sb.toString();
    }
    
    /**
     * Serialize to JSON for persistence.
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("createdAt", createdAt);
        json.put("latitude", latitude);
        json.put("longitude", longitude);
        json.put("zoom", zoom);
        json.put("altitude", altitude);
        json.put("tilt", tilt);
        json.put("rotation", rotation);
        json.put("is3DMode", is3DMode);
        
        if (geocodedAddress != null) {
            json.put("geocodedAddress", geocodedAddress);
        }
        
        if (thumbnailBase64 != null) {
            json.put("thumbnail", thumbnailBase64);
        }
        
        return json;
    }
    
    /**
     * Deserialize from JSON.
     */
    public static SavedView fromJson(JSONObject json) throws JSONException {
        SavedView view = new SavedView();
        view.id = json.getString("id");
        view.name = json.getString("name");
        view.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        view.latitude = json.getDouble("latitude");
        view.longitude = json.getDouble("longitude");
        view.zoom = json.getDouble("zoom");
        view.altitude = json.optDouble("altitude", 0);
        view.tilt = json.optDouble("tilt", 0);
        view.rotation = json.optDouble("rotation", 0);
        view.is3DMode = json.optBoolean("is3DMode", false);
        view.geocodedAddress = json.optString("geocodedAddress", null);
        view.thumbnailBase64 = json.optString("thumbnail", null);
        
        return view;
    }
    
    @Override
    public String toString() {
        return "SavedView{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", is3D=" + is3DMode +
                '}';
    }
}

