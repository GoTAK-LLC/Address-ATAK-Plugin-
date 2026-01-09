package com.gotak.address.selfgeo;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.menu.MapMenuEventListener;
import com.atakmap.android.menu.MapMenuReceiver;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Widget that displays the geocoded address for the currently selected marker.
 * Positioned in the top-right corner of the map, displaying white text that 
 * matches the size of coordinate text shown for markers.
 * 
 * When user taps a marker on the map, this widget shows the geocoded address
 * at that location.
 */
public class MarkerSelectionWidget extends AbstractWidgetMapComponent 
        implements MapWidget.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener,
        MapMenuEventListener {
    
    private static final String TAG = "MarkerSelectionWidget";
    
    // Preference keys
    public static final String PREF_SHOW_MARKER_ADDRESS = "address_show_marker_selection";
    public static final String PREF_MARKER_PHOTON_FALLBACK = "address_marker_photon_fallback";
    
    // Default values
    private static final boolean DEFAULT_SHOW_MARKER_ADDRESS = true;
    private static final boolean DEFAULT_PHOTON_FALLBACK = false;
    
    // Widget positioning - one size up to match marker coords
    private static final int FONT_SIZE = 3;  // Match marker coord text size
    private static final float HORIZONTAL_MARGIN = 16f;
    private static final float VERTICAL_MARGIN = 8f;
    
    // Colors (ARGB format)
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_RED = 0xFFFF4444;
    
    // Auto-hide timeout (milliseconds) - hide address after 10 seconds of no interaction
    private static final long AUTO_HIDE_TIMEOUT = 10000;
    
    private MapView mapView;
    private Context pluginContext;
    private SharedPreferences prefs;
    
    private LinearLayoutWidget layout;
    private TextWidget addressWidget;
    
    private ExecutorService executor;
    private Handler mainHandler;
    
    // Photon API fallback geocoder
    private ReverseGeocoder photonGeocoder;
    
    private String currentAddress = "";
    private String lastDisplayedText = "";
    private int lastDisplayedColor = 0;
    private MapItem currentSelectedItem;
    
    // Auto-hide runnable
    private Runnable autoHideRunnable;
    
    // Double-tap detection
    private static final long DOUBLE_TAP_TIMEOUT = 300;
    private long lastClickTime = 0;
    
    @Override
    protected void onCreateWidgets(Context context, android.content.Intent intent, MapView mapView) {
        Log.d(TAG, "onCreateWidgets");
        this.mapView = mapView;
        this.pluginContext = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
        this.photonGeocoder = new ReverseGeocoder();
        
        // Register preference listener
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Create auto-hide runnable
        autoHideRunnable = () -> {
            Log.d(TAG, "Auto-hiding marker address widget");
            updateWidgetVisibility(false);
            currentSelectedItem = null;
        };
        
        // Create widget in TOP RIGHT corner
        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        if (root != null) {
            layout = root.getLayout(RootLayoutWidget.TOP_RIGHT);
            
            if (layout != null) {
                addressWidget = new TextWidget("", FONT_SIZE);
                addressWidget.setName("MarkerSelectionAddressWidget");
                // Right margin to align with other top-right elements
                addressWidget.setMargins(0f, VERTICAL_MARGIN, HORIZONTAL_MARGIN, VERTICAL_MARGIN);
                addressWidget.addOnClickListener(this);
                addressWidget.setVisible(false);
                
                // Add to bottom of top-right layout (below other widgets like compass)
                layout.addChildWidget(addressWidget);
                Log.d(TAG, "Widget successfully added to TOP_RIGHT layout, child count: " + layout.getChildCount());
            } else {
                Log.e(TAG, "TOP_RIGHT layout is null!");
            }
        } else {
            Log.e(TAG, "RootLayoutWidget is null!");
        }
        
        // Register for map item click events
        Log.d(TAG, "Widget enabled: " + isWidgetEnabled());
        if (isWidgetEnabled()) {
            registerMapEventListener();
        }
    }
    
    @Override
    protected void onDestroyWidgets(Context context, MapView mapView) {
        Log.d(TAG, "onDestroyWidgets");
        
        unregisterMapEventListener();
        
        // Cancel auto-hide
        mainHandler.removeCallbacks(autoHideRunnable);
        
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        
        if (executor != null) {
            executor.shutdown();
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
    
    /**
     * Register to receive radial menu events when markers are tapped.
     * Uses MapMenuReceiver which is the reliable way to detect marker selection.
     */
    private void registerMapEventListener() {
        try {
            MapMenuReceiver receiver = MapMenuReceiver.getInstance();
            if (receiver != null) {
                receiver.addEventListener(this);
                Log.d(TAG, "Successfully registered for MapMenu events");
            } else {
                Log.e(TAG, "Failed to register - MapMenuReceiver is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering map menu listener: " + e.getMessage(), e);
        }
    }
    
    /**
     * Unregister from radial menu events.
     */
    private void unregisterMapEventListener() {
        try {
            MapMenuReceiver receiver = MapMenuReceiver.getInstance();
            if (receiver != null) {
                receiver.removeEventListener(this);
                Log.d(TAG, "Successfully unregistered from MapMenu events");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering map menu listener: " + e.getMessage(), e);
        }
    }
    
    /**
     * Called when the radial menu is shown for a map item.
     * This is the reliable way to detect when a marker is tapped/selected.
     */
    @Override
    public boolean onShowMenu(MapItem item) {
        Log.d(TAG, "onShowMenu called for item: " + (item != null ? item.getUID() : "null"));
        
        if (!isWidgetEnabled()) {
            Log.d(TAG, "Widget disabled, ignoring menu event");
            return false;
        }
        
        if (item instanceof PointMapItem) {
            PointMapItem pointItem = (PointMapItem) item;
            Log.d(TAG, "Item is PointMapItem, handling selection");
            handleMarkerSelected(pointItem);
        } else if (item != null) {
            Log.d(TAG, "Item is not PointMapItem, type: " + item.getClass().getSimpleName());
        }
        
        // Return false to allow other listeners to process the event
        return false;
    }
    
    /**
     * Called when the radial menu is hidden.
     */
    @Override
    public void onHideMenu(MapItem item) {
        Log.d(TAG, "onHideMenu called");
        // Don't hide immediately - let auto-hide timer handle it
    }
    
    /**
     * Handle when a marker is selected/tapped.
     */
    private void handleMarkerSelected(PointMapItem item) {
        if (item == null) {
            Log.w(TAG, "handleMarkerSelected called with null item");
            return;
        }
        
        GeoPoint point = item.getPoint();
        if (point == null || !point.isValid()) {
            Log.w(TAG, "Selected marker has invalid point");
            return;
        }
        
        currentSelectedItem = item;
        
        // Reset auto-hide timer
        mainHandler.removeCallbacks(autoHideRunnable);
        mainHandler.postDelayed(autoHideRunnable, AUTO_HIDE_TIMEOUT);
        
        // Get marker title for logging
        String title = item.getTitle();
        if (title == null || title.isEmpty()) {
            title = item.getUID();
        }
        Log.d(TAG, "Marker selected: " + title + " at " + point.getLatitude() + ", " + point.getLongitude());
        
        // Show "Loading..." immediately while geocoding
        updateWidget("Loading address...", COLOR_WHITE);
        
        // Perform geocoding in background
        performGeocoding(point);
    }
    
    /**
     * Perform geocoding for the given point.
     */
    private void performGeocoding(final GeoPoint point) {
        executor.execute(() -> {
            // Try ATAK's GeocodeManager first
            String formattedAddress = tryAtakGeocoder(point);
            
            // Fallback to Photon API if ATAK geocoder fails
            if (formattedAddress == null) {
                if (isPhotonFallbackEnabled()) {
                    Log.d(TAG, "ATAK geocoder failed, falling back to Photon API");
                    tryPhotonGeocoder(point);
                    return; // Photon is async
                } else {
                    Log.d(TAG, "ATAK geocoder failed, Photon fallback disabled");
                    updateWidget("No Address", COLOR_RED);
                    return;
                }
            }
            
            // ATAK geocoder succeeded
            currentAddress = formattedAddress;
            updateWidget(currentAddress, COLOR_WHITE);
        });
    }
    
    /**
     * Try to geocode using ATAK's built-in GeocodeManager.
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
     * Fallback: Try to geocode using Photon API.
     */
    private void tryPhotonGeocoder(final GeoPoint point) {
        photonGeocoder.reverseGeocode(point.getLatitude(), point.getLongitude(),
                new ReverseGeocoder.ReverseGeocodeCallback() {
            @Override
            public void onSuccess(String address) {
                Log.d(TAG, "Photon geocoding success: " + address);
                currentAddress = address;
                updateWidget(currentAddress, COLOR_WHITE);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Photon geocoding failed: " + errorMessage);
                updateWidget("No Address", COLOR_RED);
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
            }
            sb.append(thoroughfare);
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
    
    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastClickTime < DOUBLE_TAP_TIMEOUT) {
            // Double-tap detected - open settings
            Log.d(TAG, "Double-tap detected - opening settings");
            openPluginSettings();
            lastClickTime = 0;
        } else {
            // Single tap - refresh geocoding if we have a selected item
            Log.d(TAG, "Single tap on widget");
            if (currentSelectedItem instanceof PointMapItem) {
                handleMarkerSelected((PointMapItem) currentSelectedItem);
            }
        }
        
        lastClickTime = currentTime;
    }
    
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
        
        if (PREF_SHOW_MARKER_ADDRESS.equals(key)) {
            boolean enabled = isWidgetEnabled();
            Log.d(TAG, "Marker selection widget enabled: " + enabled);
            
            if (enabled) {
                registerMapEventListener();
            } else {
                unregisterMapEventListener();
                updateWidgetVisibility(false);
                currentSelectedItem = null;
            }
        }
    }
    
    private boolean isWidgetEnabled() {
        return prefs.getBoolean(PREF_SHOW_MARKER_ADDRESS, DEFAULT_SHOW_MARKER_ADDRESS);
    }
    
    private boolean isPhotonFallbackEnabled() {
        return prefs.getBoolean(PREF_MARKER_PHOTON_FALLBACK, DEFAULT_PHOTON_FALLBACK);
    }
    
    private void updateWidget(String text, int color) {
        Log.d(TAG, "updateWidget called with text: " + text);
        mainHandler.post(() -> {
            if (addressWidget != null) {
                Log.d(TAG, "Updating widget text to: " + text);
                lastDisplayedText = text;
                lastDisplayedColor = color;
                addressWidget.setText(text);
                addressWidget.setColor(color);
                addressWidget.setVisible(true);
                Log.d(TAG, "Widget visibility set to true, text set to: " + text);
            } else {
                Log.e(TAG, "addressWidget is null in updateWidget!");
            }
        });
    }
    
    private void updateWidgetVisibility(boolean visible) {
        Log.d(TAG, "updateWidgetVisibility called with: " + visible);
        mainHandler.post(() -> {
            if (addressWidget != null) {
                addressWidget.setVisible(visible);
                Log.d(TAG, "Widget visibility updated to: " + visible);
            } else {
                Log.e(TAG, "addressWidget is null in updateWidgetVisibility!");
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

