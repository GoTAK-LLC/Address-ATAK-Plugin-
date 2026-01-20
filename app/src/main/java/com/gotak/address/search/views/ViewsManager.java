package com.gotak.address.search.views;

import android.content.Context;
import android.os.Environment;

import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for saved map views.
 * Handles CRUD operations and persistence to file.
 */
public class ViewsManager {
    
    private static final String TAG = "ViewsManager";
    private static final String VIEWS_FILENAME = "saved_views.json";
    private static final int MAX_VIEWS = 50; // Maximum number of saved views
    
    private final Context context;
    private final File viewsFile;
    private final List<SavedView> views;
    private final List<ViewsChangeListener> listeners;
    
    /**
     * Listener for view changes.
     */
    public interface ViewsChangeListener {
        void onViewsChanged(List<SavedView> views);
    }
    
    public ViewsManager(Context context) {
        this.context = context;
        this.views = new ArrayList<>();
        this.listeners = new ArrayList<>();
        
        // Save to ATAK directory which persists across app restarts
        File atakDir = new File(Environment.getExternalStorageDirectory(), "atak");
        File pluginDir = new File(atakDir, "plugins/address");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        this.viewsFile = new File(pluginDir, VIEWS_FILENAME);
        Log.i(TAG, "Views file location: " + viewsFile.getAbsolutePath());
        
        loadViews();
    }
    
    /**
     * Add a listener for view changes.
     */
    public void addChangeListener(ViewsChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a change listener.
     */
    public void removeChangeListener(ViewsChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of changes.
     */
    private void notifyListeners() {
        List<SavedView> copy = new ArrayList<>(views);
        for (ViewsChangeListener listener : listeners) {
            listener.onViewsChanged(copy);
        }
    }
    
    /**
     * Load views from file.
     */
    private void loadViews() {
        views.clear();
        
        if (!viewsFile.exists()) {
            Log.i(TAG, "Views file does not exist yet: " + viewsFile.getAbsolutePath());
            return;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(viewsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            String json = sb.toString();
            Log.i(TAG, "Loading views from file, json length=" + json.length());
            
            if (json.isEmpty()) {
                Log.d(TAG, "Views file is empty");
                return;
            }
            
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SavedView view = SavedView.fromJson(obj);
                views.add(view);
                Log.d(TAG, "Loaded view: " + view.getName() + " id=" + view.getId());
            }
            Log.i(TAG, "Successfully loaded " + views.size() + " saved views from " + viewsFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error reading views file: " + e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing views JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save views to file.
     */
    private void saveViews() {
        try {
            JSONArray array = new JSONArray();
            for (SavedView view : views) {
                array.put(view.toJson());
            }
            String json = array.toString();
            
            FileWriter writer = new FileWriter(viewsFile);
            writer.write(json);
            writer.close();
            
            Log.i(TAG, "Saved " + views.size() + " views to " + viewsFile.getAbsolutePath() + ", json length=" + json.length());
        } catch (IOException e) {
            Log.e(TAG, "Error writing views file: " + e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating views JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all saved views in their current order.
     * Order is preserved as saved (supports manual reordering via drag-drop).
     */
    public List<SavedView> getViews() {
        return new ArrayList<>(views);
    }
    
    /**
     * Move a view from one position to another (for drag-drop reordering).
     * @param fromPosition Original position
     * @param toPosition New position
     */
    public void moveView(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= views.size() ||
            toPosition < 0 || toPosition >= views.size()) {
            Log.w(TAG, "Invalid move positions: from=" + fromPosition + " to=" + toPosition);
            return;
        }
        
        SavedView view = views.remove(fromPosition);
        views.add(toPosition, view);
        saveViews();
        Log.d(TAG, "Moved view from " + fromPosition + " to " + toPosition);
        // Don't notify listeners here - adapter handles UI update during drag
    }
    
    /**
     * Reorder all views based on a new list order.
     * @param reorderedViews The views in their new order
     */
    public void setViewOrder(List<SavedView> reorderedViews) {
        views.clear();
        views.addAll(reorderedViews);
        saveViews();
        notifyListeners();
        Log.i(TAG, "Views reordered, new order saved");
    }
    
    /**
     * Get the number of saved views.
     */
    public int getViewCount() {
        return views.size();
    }
    
    /**
     * Check if we can add more views.
     */
    public boolean canAddView() {
        return views.size() < MAX_VIEWS;
    }
    
    /**
     * Add a new view.
     * @return true if added, false if at capacity
     */
    public boolean addView(SavedView view) {
        if (!canAddView()) {
            Log.w(TAG, "Cannot add view - at maximum capacity (" + MAX_VIEWS + ")");
            return false;
        }
        
        views.add(view);
        saveViews();
        notifyListeners();
        Log.i(TAG, "Added view: " + view.getName());
        return true;
    }
    
    /**
     * Update an existing view.
     */
    public boolean updateView(SavedView updatedView) {
        for (int i = 0; i < views.size(); i++) {
            if (views.get(i).getId().equals(updatedView.getId())) {
                views.set(i, updatedView);
                saveViews();
                notifyListeners();
                Log.i(TAG, "Updated view: " + updatedView.getName());
                return true;
            }
        }
        Log.w(TAG, "View not found for update: " + updatedView.getId());
        return false;
    }
    
    /**
     * Delete a view by ID.
     */
    public boolean deleteView(String viewId) {
        for (int i = 0; i < views.size(); i++) {
            if (views.get(i).getId().equals(viewId)) {
                SavedView removed = views.remove(i);
                saveViews();
                notifyListeners();
                Log.i(TAG, "Deleted view: " + removed.getName());
                return true;
            }
        }
        Log.w(TAG, "View not found for delete: " + viewId);
        return false;
    }
    
    /**
     * Get a view by ID.
     */
    public SavedView getViewById(String viewId) {
        for (SavedView view : views) {
            if (view.getId().equals(viewId)) {
                return view;
            }
        }
        return null;
    }
    
    /**
     * Rename a view.
     */
    public void renameView(String viewId, String newName) {
        for (SavedView view : views) {
            if (view.getId().equals(viewId)) {
                view.setName(newName);
                saveViews();
                notifyListeners();
                Log.i(TAG, "Renamed view to: " + newName);
                return;
            }
        }
    }
    
    /**
     * Clear all views (for testing/reset).
     */
    public void clearAll() {
        views.clear();
        saveViews();
        notifyListeners();
        Log.i(TAG, "Cleared all views");
    }
    
    /**
     * Generate a default name for a new view.
     */
    public String generateDefaultName() {
        return "View " + (views.size() + 1);
    }
}

