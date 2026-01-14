package com.gotak.address.search.nearby;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.icons.UserIconDatabase;
import com.atakmap.android.icons.UserIconSet;
import com.atakmap.android.maps.MapView;
import com.gotak.address.plugin.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for managing POI icons using ATAK's built-in UserIconDatabase.
 * Uses the standard ATAK iconset (UUID: 6d781afb-89a6-4c07-b2b9-a89748b6a38f) for map markers,
 * with fallback to our drawable resources for list display.
 */
public class IconsetHelper {
    private static final String TAG = "IconsetHelper";
    
    // Standard ATAK iconset UUID - same as used by Nearby plugin
    private static final String ATAK_ICONSET_UUID = "6d781afb-89a6-4c07-b2b9-a89748b6a38f";
    
    private final Context pluginContext;
    private final Map<PointOfInterestType, Integer> drawableMap;
    private final Map<PointOfInterestType, String> iconNameMap;
    
    public IconsetHelper(Context pluginContext) {
        this.pluginContext = pluginContext;
        this.drawableMap = buildDrawableMap();
        this.iconNameMap = buildIconNameMap();
        
        Log.i(TAG, "IconsetHelper initialized with " + drawableMap.size() + " POI icons");
    }
    
    /**
     * Get UserIconDatabase instance (lazy initialization).
     * Uses MapView._mapView directly like the Nearby plugin.
     */
    private UserIconDatabase getUserIconDatabase() {
        try {
            if (MapView._mapView != null) {
                return UserIconDatabase.instance(MapView._mapView.getContext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get UserIconDatabase: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get the standard ATAK iconset (lazy initialization).
     */
    private UserIconSet getIconset() {
        UserIconDatabase db = getUserIconDatabase();
        if (db == null) {
            Log.e(TAG, "UserIconDatabase is null - MapView not ready?");
            return null;
        }
        
        try {
            // Try to get the standard ATAK iconset
            UserIconSet iconset = db.getIconSet(ATAK_ICONSET_UUID, true, true);
            if (iconset != null) {
                Log.i(TAG, "Got ATAK iconset: " + iconset.getName() + " (UUID: " + ATAK_ICONSET_UUID + ")");
                
                // List ALL icons in this iconset so we know what's available
                try {
                    java.util.List<UserIcon> allIcons = iconset.getIcons();
                    Log.w(TAG, "=== ALL ICONS IN ATAK ICONSET (" + (allIcons != null ? allIcons.size() : 0) + " icons) ===");
                    if (allIcons != null) {
                        for (UserIcon icon : allIcons) {
                            Log.w(TAG, "  ICON: " + icon.getFileName() + " | Group: " + icon.getGroup());
                        }
                    }
                    Log.w(TAG, "=== END ICON LIST ===");
                } catch (Exception e3) {
                    Log.w(TAG, "Could not list icons: " + e3.getMessage());
                }
            } else {
                Log.w(TAG, "ATAK iconset not found for UUID: " + ATAK_ICONSET_UUID);
                // List available iconsets for debugging
                try {
                    java.util.List<UserIconSet> sets = db.getIconSets(true, true);
                    Log.i(TAG, "Available iconsets: " + (sets != null ? sets.size() : 0));
                    if (sets != null) {
                        for (UserIconSet s : sets) {
                            Log.i(TAG, "  - " + s.getName() + " (UUID: " + s.getUid() + ")");
                        }
                    }
                } catch (Exception e2) {
                    Log.w(TAG, "Could not list iconsets: " + e2.getMessage());
                }
            }
            return iconset;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get iconset: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Build mapping of POI types to drawable resource IDs (for list display).
     */
    private Map<PointOfInterestType, Integer> buildDrawableMap() {
        Map<PointOfInterestType, Integer> map = new HashMap<>();
        
        map.put(PointOfInterestType.HOSPITAL, R.drawable.ic_poi_hospital);
        map.put(PointOfInterestType.PHARMACY, R.drawable.ic_poi_hospital);
        map.put(PointOfInterestType.FIRE_STATION, R.drawable.ic_poi_fire);
        map.put(PointOfInterestType.POLICE_STATION, R.drawable.ic_poi_police);
        map.put(PointOfInterestType.AIRPORT, R.drawable.ic_poi_airport);
        map.put(PointOfInterestType.HELIPORT, R.drawable.ic_poi_heliport);
        map.put(PointOfInterestType.RAILWAY_STATION, R.drawable.ic_poi_railway);
        map.put(PointOfInterestType.FERRY_TERMINAL, R.drawable.ic_poi_ferry);
        map.put(PointOfInterestType.PARKING, R.drawable.ic_poi_parking);
        map.put(PointOfInterestType.GAS_STATION, R.drawable.ic_poi_fuel);
        map.put(PointOfInterestType.BANK, R.drawable.ic_poi_bank);
        map.put(PointOfInterestType.ATM, R.drawable.ic_poi_bank);
        map.put(PointOfInterestType.SCHOOL, R.drawable.ic_poi_school);
        map.put(PointOfInterestType.RESTAURANT, R.drawable.ic_poi_restaurant);
        map.put(PointOfInterestType.HOTEL, R.drawable.ic_poi_hotel);
        map.put(PointOfInterestType.SURVEILLANCE, R.drawable.ic_poi_surveillance);
        map.put(PointOfInterestType.EMBASSY, R.drawable.ic_poi_embassy);
        map.put(PointOfInterestType.GOVERNMENT, R.drawable.ic_poi_embassy);
        map.put(PointOfInterestType.PRISON, R.drawable.ic_poi_prison);
        map.put(PointOfInterestType.COMM_TOWER, R.drawable.ic_poi_tower);
        map.put(PointOfInterestType.CELL_TOWER, R.drawable.ic_poi_tower);
        map.put(PointOfInterestType.POWER_STATION, R.drawable.ic_poi_power);
        map.put(PointOfInterestType.WATER_TOWER, R.drawable.ic_poi_water_tower);
        map.put(PointOfInterestType.POST_OFFICE, R.drawable.ic_poi_post);
        map.put(PointOfInterestType.PLACE_OF_WORSHIP, R.drawable.ic_poi_worship);
        map.put(PointOfInterestType.LIBRARY, R.drawable.ic_poi_library);
        
        return map;
    }
    
    /**
     * Build mapping of POI types to ATAK iconset image names.
     * Only includes icons that ACTUALLY EXIST in ATAK's standard iconset.
     * Icons not mapped here will render using their CoT type symbol.
     */
    private Map<PointOfInterestType, String> buildIconNameMap() {
        Map<PointOfInterestType, String> map = new HashMap<>();
        
        // Only map icons that EXIST in ATAK's standard iconset (6d781afb-89a6-4c07-b2b9-a89748b6a38f)
        // Icons not in this map will fall back to CoT type rendering
        map.put(PointOfInterestType.HOSPITAL, "hospital.png");
        map.put(PointOfInterestType.PHARMACY, "hospital.png");
        map.put(PointOfInterestType.FIRE_STATION, "firebrigade.png");
        map.put(PointOfInterestType.AIRPORT, "airport.png");
        map.put(PointOfInterestType.HELIPORT, "helipad.png");
        map.put(PointOfInterestType.RAILWAY_STATION, "railway_station_png");
        map.put(PointOfInterestType.FERRY_TERMINAL, "ferry.png");
        map.put(PointOfInterestType.PARKING, "parking.png");
        map.put(PointOfInterestType.GAS_STATION, "fuel.png");
        map.put(PointOfInterestType.BANK, "bank.png");
        map.put(PointOfInterestType.ATM, "bank.png");
        map.put(PointOfInterestType.SCHOOL, "education.png");
        map.put(PointOfInterestType.GOVERNMENT, "city.png");
        map.put(PointOfInterestType.EMBASSY, "city.png");
        map.put(PointOfInterestType.POST_OFFICE, "post_office.png");
        
        // These icons DON'T exist in ATAK's standard iconset - they'll use CoT type symbol:
        // POLICE_STATION, RESTAURANT, HOTEL, SURVEILLANCE, EMBASSY, PRISON,
        // COMM_TOWER, CELL_TOWER, POWER_STATION, WATER_TOWER, PLACE_OF_WORSHIP, LIBRARY
        
        return map;
    }

    /**
     * Get the drawable resource ID for a POI type (for list display).
     */
    public int getIconDrawableId(PointOfInterestType poiType) {
        Integer resId = drawableMap.get(poiType);
        return resId != null ? resId : R.drawable.ic_marker;
    }

    /**
     * Get a Drawable for a POI type (for list display).
     */
    public Drawable getIconDrawable(PointOfInterestType poiType) {
        int resId = getIconDrawableId(poiType);
        try {
            return ContextCompat.getDrawable(pluginContext, resId);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load icon for " + poiType.name() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get a Bitmap for a POI type (for list display).
     */
    public Bitmap getIconBitmap(PointOfInterestType poiType, int size) {
        Drawable drawable = getIconDrawable(poiType);
        if (drawable == null) {
            return null;
        }
        
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Get the CoT type for a POI type.
     */
    public String getCotType(PointOfInterestType poiType) {
        if (poiType == null) {
            return "a-n-G";
        }
        return poiType.getCotType();
    }
    
    /**
     * Get the ATAK iconset image name for a POI type.
     */
    public String getIconsetImageName(PointOfInterestType poiType) {
        // Return null if no icon mapping exists - will fall back to CoT type
        return iconNameMap.get(poiType);
    }
    
    /**
     * Get a UserIcon from ATAK's iconset for a POI type.
     * This is used for setting icons on map markers.
     * 
     * @param poiType The POI type
     * @return UserIcon from ATAK iconset, or null if not available
     */
    public UserIcon getAtakIcon(PointOfInterestType poiType) {
        UserIconSet iconset = getIconset();
        if (iconset == null) {
            Log.w(TAG, "ATAK iconset not available");
            return null;
        }
        
        String imageName = getIconsetImageName(poiType);
        try {
            UserIcon icon = iconset.getIcon(imageName);
            if (icon != null) {
                Log.d(TAG, "Got ATAK icon for " + poiType.name() + ": " + imageName + 
                      ", path=" + icon.getIconsetPath() + ", type=" + icon.get2525cType());
            }
            return icon;
        } catch (Exception e) {
            Log.w(TAG, "Failed to get ATAK icon for " + poiType.name() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the iconset path for a POI type from ATAK's UserIconDatabase.
     * This is used to set IconsetPath metadata on markers.
     * 
     * @param poiType The POI type
     * @return Iconset path string, or null if not available
     */
    public String getIconsetPath(PointOfInterestType poiType) {
        UserIcon icon = getAtakIcon(poiType);
        if (icon != null) {
            String path = icon.getIconsetPath();
            Log.d(TAG, "IconsetPath for " + poiType.name() + ": " + path);
            if (path != null && !path.isEmpty()) {
                return path;
            }
        }
        return null;
    }
    
    /**
     * Get icon bitmap from ATAK's UserIconDatabase (for map markers).
     * 
     * @param poiType The POI type
     * @return Bitmap from ATAK iconset, or null if not available
     */
    public Bitmap getAtakIconBitmap(PointOfInterestType poiType) {
        UserIcon icon = getAtakIcon(poiType);
        UserIconDatabase db = getUserIconDatabase();
        if (icon != null && db != null) {
            try {
                return db.getIconBitmap(icon.getId());
            } catch (Exception e) {
                Log.w(TAG, "Failed to get ATAK icon bitmap: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Get the 2525 CoT type from ATAK's icon if available.
     * Falls back to the POI type's built-in CoT type.
     * 
     * @param poiType The POI type
     * @return 2525C CoT type string
     */
    public String get2525cType(PointOfInterestType poiType) {
        UserIcon icon = getAtakIcon(poiType);
        if (icon != null) {
            String type = icon.get2525cType();
            if (type != null && !type.isEmpty()) {
                Log.d(TAG, "Using ATAK icon 2525c type for " + poiType.name() + ": " + type);
                return type;
            }
        }
        return getCotType(poiType);
    }
    
    /**
     * Check if ATAK iconset is available.
     */
    public boolean isAtakIconsetAvailable() {
        return getIconset() != null;
    }
    
    /**
     * Get the ATAK internal asset path for a POI type.
     * These are built-in ATAK icons (in assets/icons/) that don't require an iconset.
     * Use this for POI types that don't have icons in the standard POI iconset.
     * 
     * NOTE: For marker iconUri, use format "icons/xxx.png" NOT "asset://icons/xxx.png"
     * 
     * @param poiType The POI type
     * @return Asset path like "icons/video.png", or null if no internal icon is available
     */
    public String getInternalIconUri(PointOfInterestType poiType) {
        if (poiType == null) return null;
        
        switch (poiType) {
            // === Medical ===
            case HOSPITAL:
                return "lpticons/lpt_hospital_1.png";
            // PHARMACY uses POI iconset (hospital.png)
            
            // === Emergency Services ===
            case FIRE_STATION:
                return "lpticons/lpt_flash1.png";  // Lightning/emergency
            // POLICE_STATION uses POI iconset or CoT type
            
            // === Aviation ===
            case AIRPORT:
                return "lpticons/lpt_airplane.png";
            case HELIPORT:
                return "lpticons/lpt_helipad.png";
            
            // === Transportation ===
            case FERRY_TERMINAL:
                return "lpticons/lpt_port_civ.png";
            case PARKING:
                return "lpticons/lpt_car1.png";
            
            // === Communications/Infrastructure ===
            case COMM_TOWER:
            case CELL_TOWER:
                return "lpticons/lpt_rf.png";  // Radio frequency tower
            case POWER_STATION:
                return "lpticons/lpt_nuke.png"; // Power/nuclear symbol
            
            // === Surveillance ===
            case SURVEILLANCE:
                return "icons/viewshed_eye.png";
            
            // === Lodging ===
            case HOTEL:
                return "lpticons/lpt_house1.png";
            
            // === Government ===
            case GOVERNMENT:
                return "lpticons/lpt_house1.png";
            
            // === Info/Generic ===
            case LIBRARY:
            case POST_OFFICE:
                return "lpticons/lpt_info.png";
            
            // === Community ===
            case PLACE_OF_WORSHIP:
                return "icons/bullseye_icon.png";
            
            // === Security ===
            case PRISON:
                return "icons/alarm-geophysical.png";
            case POLICE_STATION:
                return "lpticons/lpt_blue_dot2.png";
            
            // === Infrastructure ===
            case WATER_TOWER:
                return "icons/seekermarker.png";
            
            // === Food ===
            case RESTAURANT:
                return "icons/below_maroon.png";
            
            // These will use POI iconset or CoT type:
            // RAILWAY_STATION, GAS_STATION, BANK, ATM, SCHOOL, RESTAURANT,
            // EMBASSY, GOVERNMENT, PRISON, WATER_TOWER, PLACE_OF_WORSHIP
            default:
                return null;
        }
    }
}
