package com.gotak.address.search;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages search history persistence using SharedPreferences.
 * Stores recent search results for quick access.
 */
public class SearchHistoryManager {
    private static final String TAG = "SearchHistoryManager";
    private static final String PREFS_NAME = "address_search_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY_SIZE = 10;

    private final SharedPreferences prefs;
    private final List<NominatimSearchResult> historyList;

    public SearchHistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.historyList = new ArrayList<>();
        loadHistory();
    }

    /**
     * Add a result to the history. If it already exists, move it to the top.
     */
    public void addToHistory(NominatimSearchResult result) {
        // Remove if already exists (we'll re-add at top)
        removeByPlaceId(result.getPlaceId());

        // Add at beginning
        historyList.add(0, result);

        // Trim to max size
        while (historyList.size() > MAX_HISTORY_SIZE) {
            historyList.remove(historyList.size() - 1);
        }

        saveHistory();
    }

    /**
     * Remove a specific item from history by place ID.
     */
    public void removeFromHistory(long placeId) {
        removeByPlaceId(placeId);
        saveHistory();
    }

    private void removeByPlaceId(long placeId) {
        for (int i = historyList.size() - 1; i >= 0; i--) {
            if (historyList.get(i).getPlaceId() == placeId) {
                historyList.remove(i);
            }
        }
    }

    /**
     * Clear all history.
     */
    public void clearHistory() {
        historyList.clear();
        saveHistory();
    }

    /**
     * Get all history items.
     */
    public List<NominatimSearchResult> getHistory() {
        return new ArrayList<>(historyList);
    }

    /**
     * Check if history is empty.
     */
    public boolean isEmpty() {
        return historyList.isEmpty();
    }

    private void loadHistory() {
        historyList.clear();
        String json = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                NominatimSearchResult result = NominatimSearchResult.fromJson(obj);
                historyList.add(result);
            }
            Log.d(TAG, "Loaded " + historyList.size() + " history items");
        } catch (JSONException e) {
            Log.e(TAG, "Error loading history: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try {
            JSONArray array = new JSONArray();
            for (NominatimSearchResult result : historyList) {
                array.put(result.toJson());
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
            Log.d(TAG, "Saved " + historyList.size() + " history items");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving history: " + e.getMessage());
        }
    }
}

