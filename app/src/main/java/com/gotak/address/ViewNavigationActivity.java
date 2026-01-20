package com.gotak.address;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Activity that handles custom URI scheme for view navigation.
 * 
 * URI Format:
 *   addressview://navigate?lat=40.7128&lon=-74.0060&zoom=15&tilt=45&rotation=90
 * 
 * Parameters:
 *   lat (required) - Latitude in degrees
 *   lon (required) - Longitude in degrees  
 *   zoom - Zoom level 0-21 (optional)
 *   scale - Map scale in meters/pixel (optional, alternative to zoom)
 *   tilt - Camera tilt 0-90 degrees (optional)
 *   rotation - Camera heading 0-360 degrees (optional)
 *   altitude - Altitude in meters (optional)
 * 
 * Example ADB command:
 *   adb shell am start -a android.intent.action.VIEW -d "addressview://navigate?lat=40.7128&lon=-74.0060&zoom=15&tilt=45&rotation=90"
 */
public class ViewNavigationActivity extends Activity {
    
    private static final String TAG = "ViewNavigationActivity";
    
    // Action string - must match AddressSearchDropDown.NAVIGATE_TO_POSITION
    private static final String ACTION_NAVIGATE = "com.gotak.address.NAVIGATE_TO_POSITION";
    
    // Extra keys - must match AddressSearchDropDown
    private static final String EXTRA_LATITUDE = "latitude";
    private static final String EXTRA_LONGITUDE = "longitude";
    private static final String EXTRA_ZOOM = "zoom";
    private static final String EXTRA_SCALE = "scale";
    private static final String EXTRA_TILT = "tilt";
    private static final String EXTRA_ROTATION = "rotation";
    private static final String EXTRA_ALTITUDE = "altitude";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(TAG, "ViewNavigationActivity onCreate");
        
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            Log.i(TAG, "Received URI: " + data.toString());
            handleUri(data);
        } else {
            Log.w(TAG, "No URI data received");
            Toast.makeText(this, "No navigation data", Toast.LENGTH_SHORT).show();
        }
        
        // Close this activity immediately - it's just a trampoline
        finish();
    }
    
    private void handleUri(Uri uri) {
        try {
            Log.i(TAG, "Full URI: " + uri.toString());
            Log.i(TAG, "Scheme: " + uri.getScheme());
            Log.i(TAG, "Host: " + uri.getHost());
            Log.i(TAG, "Path: " + uri.getPath());
            Log.i(TAG, "Query: " + uri.getQuery());
            
            // Parse parameters
            String latStr = uri.getQueryParameter("lat");
            String lonStr = uri.getQueryParameter("lon");
            
            Log.i(TAG, "lat param: " + latStr + ", lon param: " + lonStr);
            
            if (latStr == null || lonStr == null) {
                Log.e(TAG, "Missing required lat/lon parameters. Query params: " + uri.getQueryParameterNames());
                Toast.makeText(this, "Missing lat/lon. URI: " + uri.toString(), Toast.LENGTH_LONG).show();
                return;
            }
            
            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lonStr);
            
            // Optional parameters
            String zoomStr = uri.getQueryParameter("zoom");
            String scaleStr = uri.getQueryParameter("scale");
            String tiltStr = uri.getQueryParameter("tilt");
            String rotationStr = uri.getQueryParameter("rotation");
            String altitudeStr = uri.getQueryParameter("altitude");
            
            // Build intent for our plugin - use standard Android broadcast
            Intent navIntent = new Intent(ACTION_NAVIGATE);
            navIntent.putExtra(EXTRA_LATITUDE, latitude);
            navIntent.putExtra(EXTRA_LONGITUDE, longitude);
            
            if (zoomStr != null) {
                navIntent.putExtra(EXTRA_ZOOM, Double.parseDouble(zoomStr));
            }
            if (scaleStr != null) {
                navIntent.putExtra(EXTRA_SCALE, Double.parseDouble(scaleStr));
            }
            if (tiltStr != null) {
                navIntent.putExtra(EXTRA_TILT, Double.parseDouble(tiltStr));
            }
            if (rotationStr != null) {
                navIntent.putExtra(EXTRA_ROTATION, Double.parseDouble(rotationStr));
            }
            if (altitudeStr != null) {
                navIntent.putExtra(EXTRA_ALTITUDE, Double.parseDouble(altitudeStr));
            }
            
            Log.i(TAG, "Sending navigation to ATAK: lat=" + latitude + ", lon=" + longitude);
            
            // Try multiple approaches to reach ATAK
            
            // Approach 1: Try AtakBroadcast if available (works if ATAK loaded this plugin)
            try {
                Class<?> atakBroadcastClass = Class.forName("com.atakmap.android.ipc.AtakBroadcast");
                Object instance = atakBroadcastClass.getMethod("getInstance").invoke(null);
                if (instance != null) {
                    atakBroadcastClass.getMethod("sendBroadcast", Intent.class).invoke(instance, navIntent);
                    Log.i(TAG, "Sent via AtakBroadcast");
                    Toast.makeText(this, "Navigating to " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Log.d(TAG, "AtakBroadcast not available: " + e.getMessage());
            }
            
            // Approach 2: Try different ATAK package names
            String[] atakPackages = {
                "com.atakmap.app.civ",
                "com.atakmap.app",
                "com.atakmap.app.mil", 
                "com.atakmap.app.gov"
            };
            
            Intent launchIntent = null;
            String foundPackage = null;
            
            for (String pkg : atakPackages) {
                launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                if (launchIntent != null) {
                    foundPackage = pkg;
                    Log.i(TAG, "Found ATAK package: " + pkg);
                    break;
                }
            }
            
            if (launchIntent != null) {
                try {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(launchIntent);
                    Log.i(TAG, "Launched ATAK: " + foundPackage);
                    
                    // Give ATAK a moment to come to foreground, then try broadcast again
                    final Intent finalNavIntent = navIntent;
                    new android.os.Handler(getMainLooper()).postDelayed(() -> {
                        try {
                            // Try AtakBroadcast again now that ATAK is in foreground
                            Class<?> atakBroadcastClass = Class.forName("com.atakmap.android.ipc.AtakBroadcast");
                            Object instance = atakBroadcastClass.getMethod("getInstance").invoke(null);
                            if (instance != null) {
                                atakBroadcastClass.getMethod("sendBroadcast", Intent.class).invoke(instance, finalNavIntent);
                                Log.i(TAG, "Sent navigation via AtakBroadcast after launch");
                            }
                        } catch (Exception ex) {
                            Log.d(TAG, "Delayed AtakBroadcast failed: " + ex.getMessage());
                        }
                    }, 500);
                    
                    Toast.makeText(this, "Navigating to " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch ATAK: " + e.getMessage(), e);
                    Toast.makeText(this, "Could not launch ATAK: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "No ATAK installation found");
                Toast.makeText(this, "ATAK not found. Tried: civ, mil, gov", Toast.LENGTH_LONG).show();
            }
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid number format in URI: " + e.getMessage());
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error handling URI: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

