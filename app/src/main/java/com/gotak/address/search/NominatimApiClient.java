package com.gotak.address.search;

import android.os.Handler;
import android.os.Looper;

import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client for the Nominatim OpenStreetMap geocoding API.
 * https://nominatim.org/release-docs/develop/api/Search/
 * 
 * No API key required, but must follow usage policy:
 * - Proper User-Agent header
 * - Max 1 request per second (handled by debouncing in UI)
 */
public class NominatimApiClient {
    private static final String TAG = "NominatimApiClient";
    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "ATAK-AddressPlugin/1.0";
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds
    private static final int DEFAULT_LIMIT = 10;

    private final ExecutorService executor;
    private final Handler mainHandler;

    public NominatimApiClient() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Callback interface for search results.
     */
    public interface SearchCallback {
        void onSuccess(List<NominatimSearchResult> results);
        void onError(String errorMessage);
    }

    /**
     * Search for places matching the query.
     * Runs on a background thread and returns results via callback on the main thread.
     */
    public void search(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                List<NominatimSearchResult> results = performSearch(query);
                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                Log.e(TAG, "Search error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Perform synchronous search (must be called from background thread).
     */
    private List<NominatimSearchResult> performSearch(String query) throws IOException, JSONException {
        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String urlString = BASE_URL + "?q=" + encodedQuery 
                + "&format=json"
                + "&addressdetails=1"
                + "&limit=" + DEFAULT_LIMIT;

        Log.d(TAG, "Searching: " + urlString);

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
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parse JSON array of results
            JSONArray jsonArray = new JSONArray(response.toString());
            List<NominatimSearchResult> results = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                NominatimSearchResult result = NominatimSearchResult.fromJson(
                        jsonArray.getJSONObject(i));
                results.add(result);
            }

            Log.i(TAG, "Found " + results.size() + " results for: " + query);
            return results;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executor.shutdown();
    }
}

