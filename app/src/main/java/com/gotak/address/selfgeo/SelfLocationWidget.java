package com.gotak.address.selfgeo;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.android.widgets.AbstractWidgetMapComponent;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.android.widgets.TextWidget;
import com.atakmap.app.SettingsActivity;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Widget that displays the user's current address above their callsign on the map.
 * Uses ATAK's GeocodeManager to convert GPS coordinates to an address.
 */
public class SelfLocationWidget extends AbstractWidgetMapComponent 
        implements MapWidget.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String TAG = "SelfLocationWidget";
    
    // Preference keys
    public static final String PREF_SHOW_SELF_ADDRESS = "address_show_self_location";
    public static final String PREF_REFRESH_PERIOD = "address_refresh_period";
    public static final String PREF_PHOTON_FALLBACK = "address_photon_fallback";
    
    // Cache keys for persisting address across restarts
    private static final String CACHE_LAST_ADDRESS = "address_cache_last_address";
    private static final String CACHE_LAST_LAT = "address_cache_last_lat";
    private static final String CACHE_LAST_LON = "address_cache_last_lon";
    
    // Default values
    private static final boolean DEFAULT_SHOW_SELF_ADDRESS = true;
    private static final int DEFAULT_REFRESH_PERIOD_SECONDS = 5;
    private static final boolean DEFAULT_PHOTON_FALLBACK = false; // Off by default for privacy
    
    // Widget positioning - matches callsign area style
    private static final int FONT_SIZE = 2;
    private static final float HORIZONTAL_MARGIN = 16f;
    private static final float VERTICAL_MARGIN = 4f;
    
    // Colors (ARGB format)
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_RED = 0xFFFF4444;
    
    // Minimum distance (in meters) before re-geocoding
    // 50 feet = 15.24 meters - only geocode when user moves significantly
    private static final double MIN_DISTANCE_FOR_GEOCODE = 15.24;
    
    private MapView mapView;
    private Context pluginContext;
    private SharedPreferences prefs;
    
    private LinearLayoutWidget layout;
    private TextWidget addressWidget;
    
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> geocodingTask;
    private Handler mainHandler;
    
    // Photon API fallback geocoder
    private ReverseGeocoder photonGeocoder;
    
    private GeoPoint lastGeocodedPoint;
    private String currentAddress = "";
    private String lastDisplayedText = "";
    private int lastDisplayedColor = 0;
    
    // Double-tap detection
    private static final long DOUBLE_TAP_TIMEOUT = 300; // milliseconds
    private long lastClickTime = 0;
    
    @Override
    protected void onCreateWidgets(Context context, android.content.Intent intent, MapView mapView) {
        Log.d(TAG, "onCreateWidgets");
        this.mapView = mapView;
        this.pluginContext = context;
        // Use ATAK's context for SharedPreferences to match settings UI
        this.prefs = PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.photonGeocoder = new ReverseGeocoder();
        
        // Register preference listener
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Load cached address from previous session
        loadCachedAddress();
        
        // Create widget in bottom right corner, between server widget and callsign
        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        if (root != null) {
            // Get the BOTTOM_RIGHT layout directly
            layout = root.getLayout(RootLayoutWidget.BOTTOM_RIGHT);
            
            addressWidget = new TextWidget("", FONT_SIZE);
            addressWidget.setName("SelfAddressWidget");
            // Right margin to align with callsign, minimal vertical margins
            addressWidget.setMargins(0f, VERTICAL_MARGIN, HORIZONTAL_MARGIN, VERTICAL_MARGIN);
            addressWidget.addOnClickListener(this);
            addressWidget.setVisible(false);
            
            // Find the SelfLocTray (callsign) and insert at its position (pushing it down)
            int insertIndex = 2; // Default position
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                MapWidget child = (MapWidget) layout.getChildWidgetAt(i);
                if (child != null) {
                    String name = child.getName();
                    Log.d(TAG, "Child " + i + ": " + name);
                    // Find the callsign tray and insert at its position
                    if (name != null && name.contains("SelfLocTray")) {
                        insertIndex = i; // Insert at the callsign tray's position (pushes it down)
                        Log.d(TAG, "Found callsign tray at index " + i + ", inserting at same index");
                        break;
                    }
                }
            }
            
            Log.d(TAG, "Inserting widget at index " + insertIndex + " (total children: " + childCount + ")");
            layout.addChildWidgetAt(insertIndex, addressWidget);
        }
        
        // Start geocoding if enabled
        if (isGeocodingEnabled()) {
            startGeocoding();
        }
    }
    
    @Override
    protected void onDestroyWidgets(Context context, MapView mapView) {
        Log.d(TAG, "onDestroyWidgets");
        
        stopGeocoding();
        
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (photonGeocoder != null) {
            photonGeocoder.shutdown();
        }
        
        if (layout != null && addressWidget != null) {
            layout.removeWidget(addressWidget);
        }
        
        addressWidget = null;
        layout = null;
    }
    
    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastClickTime < DOUBLE_TAP_TIMEOUT) {
            // Double-tap detected - open settings
            Log.d(TAG, "Double-tap detected - opening settings");
            openPluginSettings();
            lastClickTime = 0; // Reset to prevent triple-tap
        } else {
            // Single tap - refresh geocoding
            Log.d(TAG, "Single tap - refreshing geocoding");
            lastGeocodedPoint = null;
            performGeocoding();
        }
        
        lastClickTime = currentTime;
    }
    
    /**
     * Open the Address plugin settings in ATAK's preferences.
     */
    private void openPluginSettings() {
        try {
            SettingsActivity.start("addressPreferences", "addressPreferences");
            Log.d(TAG, "Opened Address plugin settings");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open settings: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference changed: " + key);
        if (PREF_SHOW_SELF_ADDRESS.equals(key)) {
            boolean enabled = isGeocodingEnabled();
            Log.d(TAG, "Show My Address toggled: " + enabled);
            if (enabled) {
                // Toggled ON - load cached address, start geocoding, and force an immediate geocode
                loadCachedAddress();
                startGeocoding();
                // Force immediate geocode to get fresh address
                lastGeocodedPoint = null;
                performGeocoding();
            } else {
                // Toggled OFF - stop geocoding and hide widget immediately
                stopGeocoding();
                updateWidgetVisibility(false);
                Log.d(TAG, "Widget hidden");
            }
        } else if (PREF_REFRESH_PERIOD.equals(key)) {
            // Restart with new period
            if (isGeocodingEnabled()) {
                stopGeocoding();
                startGeocoding();
            }
        }
    }
    
    private boolean isGeocodingEnabled() {
        return prefs.getBoolean(PREF_SHOW_SELF_ADDRESS, DEFAULT_SHOW_SELF_ADDRESS);
    }
    
    private boolean isPhotonFallbackEnabled() {
        return prefs.getBoolean(PREF_PHOTON_FALLBACK, DEFAULT_PHOTON_FALLBACK);
    }
    
    private int getRefreshPeriodSeconds() {
        try {
            String value = prefs.getString(PREF_REFRESH_PERIOD, String.valueOf(DEFAULT_REFRESH_PERIOD_SECONDS));
            int seconds = Integer.parseInt(value);
            return Math.max(1, seconds); // Minimum 1 second
        } catch (Exception e) {
            return DEFAULT_REFRESH_PERIOD_SECONDS;
        }
    }
    
    private void startGeocoding() {
        Log.d(TAG, "Starting geocoding with period: " + getRefreshPeriodSeconds() + "s");
        
        if (geocodingTask != null) {
            geocodingTask.cancel(false);
        }
        
        geocodingTask = scheduler.scheduleWithFixedDelay(
                this::performGeocoding,
                0, // Initial delay
                getRefreshPeriodSeconds(),
                TimeUnit.SECONDS
        );
    }
    
    private void stopGeocoding() {
        Log.d(TAG, "Stopping geocoding");
        
        if (geocodingTask != null) {
            geocodingTask.cancel(false);
            geocodingTask = null;
        }
    }
    
    private void performGeocoding() {
        if (mapView == null) return;
        
        // Don't perform any lookups if geocoding is disabled
        if (!isGeocodingEnabled()) {
            Log.d(TAG, "Geocoding disabled, skipping lookup");
            return;
        }
        
        // Get self marker position
        PointMapItem selfMarker = mapView.getSelfMarker();
        if (selfMarker == null) {
            return;
        }
        
        GeoPoint point = selfMarker.getPoint();
        if (point == null || !point.isValid()) {
            return;
        }
        
        // Skip if we haven't moved significantly (within threshold distance)
        if (lastGeocodedPoint != null) {
            double distance = point.distanceTo(lastGeocodedPoint);
            if (distance < MIN_DISTANCE_FOR_GEOCODE) {
                return;
            }
        }
        
        Log.d(TAG, "Geocoding location: " + point.getLatitude() + ", " + point.getLongitude());
        
        // Try ATAK's GeocodeManager first (better accuracy with house numbers)
        String formattedAddress = tryAtakGeocoder(point);
        
        // Fallback to Photon API if ATAK geocoder fails (only if enabled for privacy)
        if (formattedAddress == null) {
            if (isPhotonFallbackEnabled()) {
                Log.d(TAG, "ATAK geocoder failed, falling back to Photon API");
                tryPhotonGeocoder(point);
                return; // Photon is async, will update widget when done
            } else {
                Log.d(TAG, "ATAK geocoder failed, Photon fallback disabled for privacy");
                // Show "No Address" in red if we don't have a cached address
                if (currentAddress == null || currentAddress.isEmpty()) {
                    updateWidget("No Address", COLOR_RED);
                }
                return;
            }
        }
        
        // ATAK geocoder succeeded
        lastGeocodedPoint = point;
        
        if (!formattedAddress.equals(currentAddress)) {
            Log.d(TAG, "Address changed, updating widget");
            currentAddress = formattedAddress;
            saveCachedAddress(currentAddress, point);
            updateWidget(currentAddress, COLOR_CYAN);
        }
    }
    
    /**
     * Try to geocode using ATAK's built-in GeocodeManager.
     * Returns the formatted address, or null if geocoding failed.
     */
    private String tryAtakGeocoder(GeoPoint point) {
        try {
            GeocodeManager geocodeManager = GeocodeManager.getInstance(mapView.getContext());
            GeocodeManager.Geocoder geocoder = geocodeManager.getSelectedGeocoder();
            
            if (geocoder == null) {
                Log.w(TAG, "No ATAK geocoder available");
                return null;
            }
            
            if (!geocoder.testServiceAvailable()) {
                Log.w(TAG, "ATAK geocoder service not available");
                return null;
            }
            
            // This call may block, but we're on a background thread
            List<Address> addresses = geocoder.getLocation(point);
            
            if (addresses == null || addresses.isEmpty()) {
                Log.w(TAG, "No addresses returned from ATAK geocoder");
                return null;
            }
            
            Address address = addresses.get(0);
            String formattedAddress = formatAddressFromAndroid(address);
            
            Log.d(TAG, "ATAK geocoding success: " + formattedAddress);
            return formattedAddress;
            
        } catch (Exception e) {
            Log.e(TAG, "ATAK geocoding error: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Fallback: Try to geocode using Photon API (free OpenStreetMap-based service).
     * This is async and will update the widget when complete.
     */
    private void tryPhotonGeocoder(final GeoPoint point) {
        photonGeocoder.reverseGeocode(point.getLatitude(), point.getLongitude(),
                new ReverseGeocoder.ReverseGeocodeCallback() {
            @Override
            public void onSuccess(String address) {
                Log.d(TAG, "Photon geocoding success: " + address);
                lastGeocodedPoint = point;
                
                if (!address.equals(currentAddress)) {
                    currentAddress = address;
                    saveCachedAddress(currentAddress, point);
                    updateWidget(currentAddress, COLOR_CYAN);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Photon geocoding failed: " + errorMessage);
                // Both geocoders failed - show "No Address" in red if no cached address
                if (currentAddress == null || currentAddress.isEmpty()) {
                    updateWidget("No Address", COLOR_RED);
                }
            }
        });
    }
    
    /**
     * Format an Android Address object for display.
     * Two-line format: Street on first line, Town/City, Region on second line.
     */
    private String formatAddressFromAndroid(Address address) {
        if (address == null) {
            return "Unknown location";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Get individual components first - more reliable than parsing addressLine
        String subThoroughfare = address.getSubThoroughfare(); // House number
        String thoroughfare = address.getThoroughfare();       // Street name
        String featureName = address.getFeatureName();         // POI name or number
        String locality = address.getLocality();               // City/Town (e.g., Aranjuez)
        String subLocality = address.getSubLocality();         // District/neighborhood
        String adminArea = address.getAdminArea();             // State/Region (e.g., Madrid)
        String countryCode = address.getCountryCode();
        
        Log.d(TAG, "Address components - thoroughfare: " + thoroughfare + 
                   ", subThoroughfare: " + subThoroughfare +
                   ", locality: " + locality + 
                   ", subLocality: " + subLocality +
                   ", adminArea: " + adminArea);
        
        // Line 1: Street address
        if (thoroughfare != null && !thoroughfare.isEmpty()) {
            if (subThoroughfare != null && !subThoroughfare.isEmpty()) {
                sb.append(subThoroughfare).append(" ");
            } else if (featureName != null && !featureName.isEmpty() && 
                       !featureName.equals(thoroughfare)) {
                // featureName might be the house number
                try {
                    Integer.parseInt(featureName);
                    sb.append(featureName).append(" ");
                } catch (NumberFormatException e) {
                    // Not a number, skip
                }
            }
            sb.append(thoroughfare);
        } else if (featureName != null && !featureName.isEmpty()) {
            sb.append(featureName);
        } else {
            // Fallback to first part of addressLine if no thoroughfare
            String line0 = address.getAddressLine(0);
            if (line0 != null && !line0.isEmpty()) {
                String[] parts = line0.split(",");
                if (parts.length > 0) {
                    sb.append(parts[0].trim());
                }
            }
        }
        
        // Line 2: Town/City, Region, Country
        StringBuilder line2 = new StringBuilder();
        
        // Add locality (town/city) - e.g., "Aranjuez"
        if (locality != null && !locality.isEmpty()) {
            line2.append(locality);
        } else if (subLocality != null && !subLocality.isEmpty()) {
            // Fallback to subLocality if no locality
            line2.append(subLocality);
        }
        
        // Add admin area (state/region) - e.g., "Madrid"
        if (adminArea != null && !adminArea.isEmpty()) {
            // Only add if different from locality
            if (!adminArea.equalsIgnoreCase(locality)) {
                if (line2.length() > 0) line2.append(", ");
                line2.append(adminArea);
            }
        }
        
        // Add country code at the end - e.g., "ES"
        if (countryCode != null && !countryCode.isEmpty()) {
            if (line2.length() > 0) line2.append(", ");
            line2.append(countryCode.toUpperCase());
        }
        
        if (line2.length() > 0) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(line2);
        }
        
        String result = sb.toString();
        return result.isEmpty() ? "Unknown location" : result;
    }
    
    /**
     * Load cached address from SharedPreferences.
     * Shows the last known address immediately while waiting for fresh geocoding.
     */
    private void loadCachedAddress() {
        // Don't load or show cached address if feature is disabled
        if (!isGeocodingEnabled()) {
            Log.d(TAG, "Geocoding disabled, not loading cached address");
            return;
        }
        
        String cachedAddress = prefs.getString(CACHE_LAST_ADDRESS, null);
        float cachedLat = prefs.getFloat(CACHE_LAST_LAT, 0f);
        float cachedLon = prefs.getFloat(CACHE_LAST_LON, 0f);
        
        if (cachedAddress != null && !cachedAddress.isEmpty()) {
            Log.d(TAG, "Loaded cached address: " + cachedAddress);
            currentAddress = cachedAddress;
            
            // Restore the last geocoded point to avoid unnecessary re-geocoding
            if (cachedLat != 0f || cachedLon != 0f) {
                lastGeocodedPoint = new GeoPoint(cachedLat, cachedLon);
            }
            
            // Show cached address immediately
            updateWidget(currentAddress, COLOR_CYAN);
        }
    }
    
    /**
     * Save address to cache for persistence across restarts.
     */
    private void saveCachedAddress(String address, GeoPoint point) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CACHE_LAST_ADDRESS, address);
        if (point != null) {
            editor.putFloat(CACHE_LAST_LAT, (float) point.getLatitude());
            editor.putFloat(CACHE_LAST_LON, (float) point.getLongitude());
        }
        editor.apply();
        Log.d(TAG, "Cached address saved");
    }
    
    /**
     * Convert country name to 2-letter code.
     */
    private String getCountryCode(String countryName) {
        if (countryName == null) return "";
        
        // If already a 2-letter code, return as is
        if (countryName.length() == 2) return countryName.toUpperCase();
        
        String lower = countryName.toLowerCase().trim();
        switch (lower) {
            case "spain": case "españa": return "ES";
            case "united states": case "united states of america": case "usa": return "US";
            case "united kingdom": case "uk": case "great britain": return "UK";
            case "france": return "FR";
            case "germany": case "deutschland": return "DE";
            case "italy": case "italia": return "IT";
            case "portugal": return "PT";
            case "canada": return "CA";
            case "australia": return "AU";
            default:
                // Use first 2 letters as fallback
                return countryName.length() >= 2 ? countryName.substring(0, 2).toUpperCase() : countryName;
        }
    }
    
    private void updateWidget(String text, int color) {
        mainHandler.post(() -> {
            if (addressWidget != null) {
                // Only update if text or color has changed to avoid UI blinking
                if (text.equals(lastDisplayedText) && color == lastDisplayedColor) {
                    return;
                }
                lastDisplayedText = text;
                lastDisplayedColor = color;
                addressWidget.setText(text);
                addressWidget.setColor(color);
                updateWidgetVisibility(true);
            }
        });
    }
    
    private void updateWidgetVisibility(boolean visible) {
        mainHandler.post(() -> {
            if (addressWidget != null) {
                addressWidget.setVisible(visible);
            }
        });
    }
    
    /**
     * Public method to dispose the widget from outside.
     */
    public void dispose() {
        if (mapView != null && pluginContext != null) {
            onDestroyWidgets(pluginContext, mapView);
        }
    }
}
