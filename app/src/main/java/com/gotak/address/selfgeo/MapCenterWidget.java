package com.gotak.address.selfgeo;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

import com.atakmap.android.maps.MapView;
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
 * Widget that displays the geocoded address for the map center crosshairs.
 * Only active when ATAK's "Designate Map Centre" setting is enabled.
 * Positioned in the bottom-left corner of the map.
 */
public class MapCenterWidget extends AbstractWidgetMapComponent 
        implements MapWidget.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String TAG = "MapCenterWidget";
    
    // ATAK's preference key for showing the map center crosshairs
    private static final String ATAK_PREF_MAP_CENTER_DESIGNATOR = "map_center_designator";
    
    // Plugin preference keys
    public static final String PREF_SHOW_MAP_CENTER_ADDRESS = "address_show_map_center";
    public static final String PREF_MAP_CENTER_PHOTON_FALLBACK = "address_map_center_photon_fallback";
    
    // Default values
    private static final boolean DEFAULT_SHOW_MAP_CENTER_ADDRESS = true;
    private static final boolean DEFAULT_PHOTON_FALLBACK = false;
    
    // Widget positioning
    private static final int FONT_SIZE = 2;
    private static final float HORIZONTAL_MARGIN = 16f;
    private static final float VERTICAL_MARGIN = 4f;
    
    // Colors (ARGB format)
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_RED = 0xFFFF4444;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    
    // Refresh rate (1 second like tak-geocoder-plugin)
    private static final int REFRESH_PERIOD_MS = 1000;
    
    // Minimum distance (meters) before re-geocoding to avoid excessive API calls
    private static final double MIN_DISTANCE_FOR_GEOCODE = 50.0;
    
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
    private static final long DOUBLE_TAP_TIMEOUT = 300;
    private long lastClickTime = 0;
    
    @Override
    protected void onCreateWidgets(Context context, android.content.Intent intent, MapView mapView) {
        Log.d(TAG, "onCreateWidgets");
        this.mapView = mapView;
        this.pluginContext = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(MapView.getMapView().getContext());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.photonGeocoder = new ReverseGeocoder();
        
        // Register preference listener
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        // Create widget in bottom left corner (like tak-geocoder-plugin)
        RootLayoutWidget root = (RootLayoutWidget) mapView.getComponentExtra("rootLayoutWidget");
        if (root != null) {
            layout = root.getLayout(RootLayoutWidget.BOTTOM_LEFT);
            
            addressWidget = new TextWidget("", FONT_SIZE);
            addressWidget.setName("MapCenterAddressWidget");
            addressWidget.setMargins(HORIZONTAL_MARGIN, VERTICAL_MARGIN, 0f, VERTICAL_MARGIN);
            addressWidget.addOnClickListener(this);
            addressWidget.setVisible(false);
            
            // Add to bottom-left layout
            layout.addChildWidget(addressWidget);
        }
        
        // Always start the geocoding task - it will check shouldShowWidget() each cycle
        // This ensures we detect when crosshairs are enabled/disabled
        startGeocoding();
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
            lastClickTime = 0;
        } else {
            // Single tap - refresh geocoding immediately
            Log.d(TAG, "Single tap - refreshing geocoding");
            lastGeocodedPoint = null;
            performGeocoding();
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
        
        if (PREF_SHOW_MAP_CENTER_ADDRESS.equals(key) || ATAK_PREF_MAP_CENTER_DESIGNATOR.equals(key)) {
            boolean shouldShow = shouldShowWidget();
            Log.d(TAG, "Map center widget enabled: " + shouldShow);
            
            if (shouldShow) {
                // Force immediate geocode when enabled
                lastGeocodedPoint = null;
                performGeocoding();
            } else {
                // Immediately hide the widget when disabled
                updateWidgetVisibility(false);
                currentAddress = "";
                lastDisplayedText = "";
                lastGeocodedPoint = null;
            }
        }
    }
    
    /**
     * Check if the widget should be shown.
     * Requires both ATAK's map center designator AND the plugin preference to be enabled.
     */
    private boolean shouldShowWidget() {
        boolean atakCenterEnabled = prefs.getBoolean(ATAK_PREF_MAP_CENTER_DESIGNATOR, false);
        boolean pluginEnabled = prefs.getBoolean(PREF_SHOW_MAP_CENTER_ADDRESS, DEFAULT_SHOW_MAP_CENTER_ADDRESS);
        return atakCenterEnabled && pluginEnabled;
    }
    
    private boolean isPhotonFallbackEnabled() {
        return prefs.getBoolean(PREF_MAP_CENTER_PHOTON_FALLBACK, DEFAULT_PHOTON_FALLBACK);
    }
    
    private void startGeocoding() {
        Log.d(TAG, "Starting map center geocoding");
        
        if (geocodingTask != null) {
            geocodingTask.cancel(false);
        }
        
        geocodingTask = scheduler.scheduleWithFixedDelay(
                this::performGeocoding,
                0,
                REFRESH_PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
    }
    
    private void stopGeocoding() {
        Log.d(TAG, "Stopping map center geocoding");
        
        if (geocodingTask != null) {
            geocodingTask.cancel(false);
            geocodingTask = null;
        }
    }
    
    private void performGeocoding() {
        if (mapView == null) return;
        
        // Hide widget and skip geocoding if crosshairs are disabled
        if (!shouldShowWidget()) {
            Log.d(TAG, "Widget disabled, hiding and skipping geocode");
            updateWidgetVisibility(false);
            currentAddress = "";
            lastGeocodedPoint = null;
            return;
        }
        
        // Get the map center point (crosshairs position)
        GeoPoint point = mapView.getCenterPoint().get();
        if (point == null || !point.isValid()) {
            Log.d(TAG, "Invalid map center point");
            return;
        }
        
        // Skip if the map center hasn't moved significantly
        if (lastGeocodedPoint != null) {
            double distance = point.distanceTo(lastGeocodedPoint);
            if (distance < MIN_DISTANCE_FOR_GEOCODE) {
                return;
            }
        }
        
        Log.d(TAG, "Geocoding map center: " + point.getLatitude() + ", " + point.getLongitude());
        
        // Try ATAK's GeocodeManager first
        String formattedAddress = tryAtakGeocoder(point);
        
        // Fallback to Photon API if ATAK geocoder fails
        if (formattedAddress == null) {
            if (isPhotonFallbackEnabled()) {
                Log.d(TAG, "ATAK geocoder failed, falling back to Photon API");
                tryPhotonGeocoder(point);
                return;
            } else {
                Log.d(TAG, "ATAK geocoder failed, Photon fallback disabled");
                if (currentAddress == null || currentAddress.isEmpty()) {
                    updateWidget("No Address", COLOR_RED);
                }
                return;
            }
        }
        
        // ATAK geocoder succeeded
        lastGeocodedPoint = point;
        
        if (!formattedAddress.equals(currentAddress)) {
            Log.d(TAG, "Map center address changed");
            currentAddress = formattedAddress;
            updateWidget(currentAddress, COLOR_YELLOW);
        }
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
                lastGeocodedPoint = point;
                
                if (!address.equals(currentAddress)) {
                    currentAddress = address;
                    updateWidget(currentAddress, COLOR_YELLOW);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Photon geocoding failed: " + errorMessage);
                if (currentAddress == null || currentAddress.isEmpty()) {
                    updateWidget("No Address", COLOR_RED);
                }
            }
        });
    }
    
    /**
     * Format an Android Address object for display.
     */
    private String formatAddressFromAndroid(Address address) {
        if (address == null) {
            return "Unknown location";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // First try to get address lines
        String line0 = address.getAddressLine(0);
        if (line0 != null && !line0.isEmpty()) {
            String[] parts = line0.split(",");
            
            // Street address
            if (parts.length > 0) {
                sb.append(parts[0].trim());
            }
            
            // City
            if (parts.length > 1) {
                sb.append("\n").append(parts[1].trim());
            }
            
            // State/country
            if (parts.length > 2) {
                String lastPart = parts[parts.length - 1].trim();
                String countryCode = getCountryCode(lastPart);
                if (parts.length > 3) {
                    String state = parts[parts.length - 2].trim();
                    sb.append("\n").append(state).append(", ").append(countryCode);
                } else {
                    sb.append("\n").append(countryCode);
                }
            }
            
            return sb.toString();
        }
        
        // Fallback: Build from individual components
        String subThoroughfare = address.getSubThoroughfare();
        String thoroughfare = address.getThoroughfare();
        String featureName = address.getFeatureName();
        
        if (thoroughfare != null && !thoroughfare.isEmpty()) {
            if (subThoroughfare != null && !subThoroughfare.isEmpty()) {
                sb.append(subThoroughfare).append(" ");
            } else if (featureName != null && !featureName.isEmpty() && 
                       !featureName.equals(thoroughfare)) {
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
        }
        
        String locality = address.getLocality();
        if (locality != null && !locality.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(locality);
        }
        
        String adminArea = address.getAdminArea();
        String countryCode = address.getCountryCode();
        
        if (adminArea != null || countryCode != null) {
            if (sb.length() > 0) sb.append("\n");
            if (adminArea != null && !adminArea.isEmpty()) {
                sb.append(adminArea);
                if (countryCode != null && !countryCode.isEmpty()) {
                    sb.append(", ").append(countryCode);
                }
            } else if (countryCode != null) {
                sb.append(countryCode);
            }
        }
        
        String result = sb.toString();
        return result.isEmpty() ? "Unknown location" : result;
    }
    
    /**
     * Convert country name to 2-letter code.
     */
    private String getCountryCode(String countryName) {
        if (countryName == null) return "";
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
                return countryName.length() >= 2 ? countryName.substring(0, 2).toUpperCase() : countryName;
        }
    }
    
    private void updateWidget(String text, int color) {
        mainHandler.post(() -> {
            if (addressWidget != null) {
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

