package com.gotak.address.selfgeo;

import android.os.Handler;
import android.os.Looper;

import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Performs reverse geocoding (coordinates to address) using the Photon API.
 * Falls back to Nominatim if Photon fails.
 */
public class ReverseGeocoder {
    private static final String TAG = "ReverseGeocoder";
    
    // Photon API for reverse geocoding
    private static final String PHOTON_REVERSE_URL = "https://photon.komoot.io/reverse";
    
    // Nominatim API as fallback
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    
    private static final String USER_AGENT = "ATAK-AddressPlugin/1.0";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    public ReverseGeocoder() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Callback interface for reverse geocoding results.
     */
    public interface ReverseGeocodeCallback {
        void onSuccess(String address);
        void onError(String errorMessage);
    }
    
    /**
     * Reverse geocode coordinates to an address string.
     */
    public void reverseGeocode(double latitude, double longitude, ReverseGeocodeCallback callback) {
        executor.execute(() -> {
            try {
                // Try Photon first
                String address = performPhotonReverse(latitude, longitude);
                
                if (address == null || address.isEmpty()) {
                    // Fall back to Nominatim
                    Log.d(TAG, "Photon returned no results, trying Nominatim");
                    address = performNominatimReverse(latitude, longitude);
                }
                
                final String finalAddress = address;
                if (finalAddress != null && !finalAddress.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(finalAddress));
                } else {
                    mainHandler.post(() -> callback.onError("No address found"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Reverse geocoding error", e);
                // Try Nominatim as fallback
                try {
                    String address = performNominatimReverse(latitude, longitude);
                    if (address != null && !address.isEmpty()) {
                        final String fallbackAddress = address;
                        mainHandler.post(() -> callback.onSuccess(fallbackAddress));
                    } else {
                        mainHandler.post(() -> callback.onError(e.getMessage()));
                    }
                } catch (Exception e2) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }
    
    /**
     * Perform Photon reverse geocoding.
     */
    private String performPhotonReverse(double latitude, double longitude) throws IOException, JSONException {
        String urlString = String.format(Locale.US, 
                "%s?lat=%.6f&lon=%.6f&limit=1",
                PHOTON_REVERSE_URL, latitude, longitude);
        
        Log.d(TAG, "Photon reverse: " + urlString);
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Photon HTTP error: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Parse GeoJSON response
            JSONObject json = new JSONObject(response.toString());
            JSONArray features = json.getJSONArray("features");
            
            if (features.length() == 0) {
                return null;
            }
            
            JSONObject properties = features.getJSONObject(0).getJSONObject("properties");
            return buildAddressFromPhoton(properties);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Build a formatted address from Photon properties.
     */
    private String buildAddressFromPhoton(JSONObject properties) {
        StringBuilder sb = new StringBuilder();
        
        // Log all properties for debugging
        Log.d(TAG, "Photon properties: " + properties.toString());
        
        String name = properties.optString("name", null);
        String housenumber = properties.optString("housenumber", null);
        String street = properties.optString("street", null);
        String city = properties.optString("city", null);
        String state = properties.optString("state", null);
        String country = properties.optString("country", null);
        
        // Also check alternative field names that Photon might use
        if (street == null || street.isEmpty()) {
            street = properties.optString("road", null);
        }
        if (city == null || city.isEmpty()) {
            city = properties.optString("locality", null);
        }
        if (city == null || city.isEmpty()) {
            city = properties.optString("town", null);
        }
        if (city == null || city.isEmpty()) {
            city = properties.optString("village", null);
        }
        if (city == null || city.isEmpty()) {
            city = properties.optString("district", null);
        }
        
        Log.d(TAG, "Parsed - street: " + street + ", housenumber: " + housenumber + 
                   ", city: " + city + ", name: " + name);
        
        // Line 1: Always prioritize street address with house number
        boolean hasStreetAddress = false;
        if (street != null && !street.isEmpty()) {
            if (housenumber != null && !housenumber.isEmpty()) {
                sb.append(housenumber).append(" ");
            }
            sb.append(street);
            hasStreetAddress = true;
        }
        
        // Add name on second line only if it's different and meaningful
        // (not just repeating the street or city)
        if (name != null && !name.isEmpty()) {
            boolean nameIsDifferent = true;
            if (street != null && name.equalsIgnoreCase(street)) {
                nameIsDifferent = false;
            }
            if (city != null && name.equalsIgnoreCase(city)) {
                nameIsDifferent = false;
            }
            
            if (nameIsDifferent) {
                if (hasStreetAddress) {
                    // Name is secondary info (building/POI name)
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(name);
                } else {
                    // No street, use name as primary
                    sb.append(name);
                }
            }
        }
        
        // Line: City
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(city);
        }
        
        // Line: State/Country (compact)
        String location = "";
        if (state != null && !state.isEmpty()) {
            location = state;
        }
        if (country != null && !country.isEmpty()) {
            String countryCode = getCountryCode(country);
            if (!location.isEmpty()) {
                location += ", " + countryCode;
            } else {
                location = countryCode;
            }
        }
        if (!location.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(location);
        }
        
        String result = sb.toString();
        Log.d(TAG, "Built address: " + result);
        return result;
    }
    
    /**
     * Perform Nominatim reverse geocoding as fallback.
     */
    private String performNominatimReverse(double latitude, double longitude) throws IOException, JSONException {
        String urlString = String.format(Locale.US,
                "%s?lat=%.6f&lon=%.6f&format=json&addressdetails=1",
                NOMINATIM_REVERSE_URL, latitude, longitude);
        
        Log.d(TAG, "Nominatim reverse: " + urlString);
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Nominatim HTTP error: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            JSONObject json = new JSONObject(response.toString());
            return buildAddressFromNominatim(json);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Build a formatted address from Nominatim response.
     */
    private String buildAddressFromNominatim(JSONObject json) {
        JSONObject address = json.optJSONObject("address");
        if (address == null) {
            return json.optString("display_name", null);
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Line 1: Always prioritize road with house number
        String road = address.optString("road", null);
        String houseNumber = address.optString("house_number", null);
        String building = address.optString("building", null);
        
        boolean hasStreetAddress = false;
        if (road != null && !road.isEmpty()) {
            if (houseNumber != null && !houseNumber.isEmpty()) {
                sb.append(houseNumber).append(" ");
            }
            sb.append(road);
            hasStreetAddress = true;
        }
        
        // Add building name on next line if different from road
        if (building != null && !building.isEmpty()) {
            if (!building.equalsIgnoreCase(road)) {
                if (hasStreetAddress) {
                    sb.append("\n").append(building);
                } else {
                    sb.append(building);
                }
            }
        }
        
        // Line 2: City/town
        String city = address.optString("city", null);
        if (city == null) city = address.optString("town", null);
        if (city == null) city = address.optString("village", null);
        if (city == null) city = address.optString("municipality", null);
        
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(city);
        }
        
        // Line 3: State, Country code
        String state = address.optString("state", null);
        String country = address.optString("country", null);
        String countryCode = address.optString("country_code", "").toUpperCase();
        
        String location = "";
        if (state != null && !state.isEmpty()) {
            location = state;
        }
        if (!countryCode.isEmpty()) {
            if (!location.isEmpty()) {
                location += ", " + countryCode;
            } else if (country != null) {
                location = getCountryCode(country);
            }
        }
        
        if (!location.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(location);
        }
        
        return sb.length() > 0 ? sb.toString() : json.optString("display_name", "Unknown location");
    }
    
    /**
     * Convert country name to 2-letter code.
     */
    private String getCountryCode(String countryName) {
        if (countryName == null) return "";
        
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
            case "netherlands": case "nederland": return "NL";
            case "belgium": case "belgique": return "BE";
            case "switzerland": case "schweiz": case "suisse": return "CH";
            case "austria": case "österreich": return "AT";
            case "poland": case "polska": return "PL";
            case "sweden": case "sverige": return "SE";
            case "norway": case "norge": return "NO";
            case "denmark": case "danmark": return "DK";
            case "finland": case "suomi": return "FI";
            case "ireland": case "éire": return "IE";
            case "japan": return "JP";
            case "china": return "CN";
            case "india": return "IN";
            case "brazil": case "brasil": return "BR";
            case "mexico": case "méxico": return "MX";
            case "russia": return "RU";
            default:
                // Use first 2 letters as fallback
                return countryName.length() >= 2 ? countryName.substring(0, 2).toUpperCase() : countryName;
        }
    }
    
    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        executor.shutdown();
    }
}

