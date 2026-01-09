package com.gotak.address.search;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ViewFlipper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.bloodhound.BloodHoundTool;
import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.Marker;
import com.gotak.address.plugin.R;
import com.gotak.address.search.nearby.IconsetHelper;
import com.gotak.address.search.nearby.NearbyResultsAdapter;
import com.gotak.address.search.nearby.OverpassApiClient;
import com.gotak.address.search.nearby.OverpassSearchResult;
import com.gotak.address.search.nearby.PointOfInterestType;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DropDown receiver for the address search panel.
 * Provides location search using Nominatim (OpenStreetMap) API and
 * nearby POI search using Overpass API.
 */
public class AddressSearchDropDown extends DropDownReceiver implements
        DropDown.OnStateListener,
        SearchResultsAdapter.OnResultClickListener,
        HistoryAdapter.HistoryItemListener,
        NearbyResultsAdapter.OnResultClickListener {

    public static final String TAG = "AddressSearchDropDown";
    public static final String SHOW_SEARCH = "com.gotak.address.SHOW_ADDRESS_SEARCH";
    public static final String HIDE_SEARCH = "com.gotak.address.HIDE_ADDRESS_SEARCH";

    private static final long DEBOUNCE_DELAY_MS = 300;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final String PREFS_NAME = "address_search_prefs";
    private static final String PREF_SELECTED_CATEGORIES = "selected_poi_categories";
    private static final String PREF_SEARCH_RADIUS_INDEX = "search_radius_index";

    // Radius options in kilometers
    private static final int[] RADIUS_VALUES = {1, 2, 5, 10, 20, 50, 100};

    private final Context pluginContext;
    private final NominatimApiClient apiClient;
    private final OverpassApiClient overpassClient;
    private final IconsetHelper iconsetHelper;
    private final Handler mainHandler;
    private final SearchHistoryManager historyManager;
    private final SharedPreferences prefs;

    // UI elements - Root
    private View rootView;
    private ViewFlipper tabFlipper;
    private ImageButton closeButton;

    // Tab UI elements
    private TextView tabAddress;
    private TextView tabNearby;
    private View tabIndicatorAddress;
    private View tabIndicatorNearby;

    // Address Tab UI elements
    private EditText searchInput;
    private ImageButton clearButton;
    private TextView searchStatus;
    private TextView sectionHeader;
    private RecyclerView resultsRecyclerView;
    private SearchResultsAdapter resultsAdapter;
    private LinearLayout historyContainer;
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private TextView clearHistoryButton;
    private Button offlineDataButton;

    // Nearby Tab UI elements
    private Button selectCategoriesButton;
    private ImageButton radiusDecreaseButton;
    private ImageButton radiusIncreaseButton;
    private TextView radiusValueText;
    private TextView locationSelfButton;
    private TextView locationMapButton;
    private Button nearbySearchButton;
    private android.widget.ProgressBar nearbySearchSpinner;
    private TextView nearbyStatus;
    private LinearLayout nearbyResultsHeaderContainer;
    private TextView nearbyResultsHeader;
    private TextView selectAllButton;
    private TextView deselectAllButton;
    private RecyclerView nearbyResultsRecyclerView;
    private Button addToMapButton;
    private CheckBox broadcastCheckbox;
    private NearbyResultsAdapter nearbyResultsAdapter;

    // State
    private int currentTab = 0; // 0 = Address, 1 = Nearby
    private Set<PointOfInterestType> selectedCategories = new HashSet<>();
    private int radiusIndex = 2; // Default to 5km
    private boolean useMapCenter = false; // false = My Location, true = Map Center

    // Debounce handling
    private final Runnable searchRunnable;
    private String pendingQuery = "";

    public AddressSearchDropDown(MapView mapView, Context pluginContext) {
        super(mapView);
        this.pluginContext = pluginContext;
        this.apiClient = new NominatimApiClient(pluginContext);
        this.overpassClient = new OverpassApiClient(pluginContext);
        this.iconsetHelper = new IconsetHelper(pluginContext);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.historyManager = new SearchHistoryManager(pluginContext);
        this.prefs = pluginContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved preferences
        loadPreferences();

        // Create debounced search runnable
        this.searchRunnable = () -> {
            if (pendingQuery.length() >= MIN_QUERY_LENGTH) {
                performSearch(pendingQuery);
            }
        };
    }

    private void loadPreferences() {
        // Load saved categories
        Set<String> savedCategories = prefs.getStringSet(PREF_SELECTED_CATEGORIES, null);
        if (savedCategories != null) {
            for (String catName : savedCategories) {
                try {
                    selectedCategories.add(PointOfInterestType.valueOf(catName));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // Load saved radius
        radiusIndex = prefs.getInt(PREF_SEARCH_RADIUS_INDEX, 2);
        if (radiusIndex < 0) radiusIndex = 0;
        if (radiusIndex >= RADIUS_VALUES.length) radiusIndex = RADIUS_VALUES.length - 1;
    }

    private void savePreferences() {
        Set<String> categoryNames = new HashSet<>();
        for (PointOfInterestType type : selectedCategories) {
            categoryNames.add(type.name());
        }
        prefs.edit()
            .putStringSet(PREF_SELECTED_CATEGORIES, categoryNames)
            .putInt(PREF_SEARCH_RADIUS_INDEX, radiusIndex)
            .apply();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "onReceive: " + action);

        switch (action) {
            case SHOW_SEARCH:
                showSearchPanel();
                break;
            case HIDE_SEARCH:
                closeDropDown();
                break;
        }
    }

    private void showSearchPanel() {
        try {
            // Inflate layout
            LayoutInflater inflater = LayoutInflater.from(pluginContext);
            rootView = inflater.inflate(R.layout.address_search_panel, null);

            // Find tab-related views
            tabFlipper = rootView.findViewById(R.id.tab_flipper);
            closeButton = rootView.findViewById(R.id.close_button);
            tabAddress = rootView.findViewById(R.id.tab_address);
            tabNearby = rootView.findViewById(R.id.tab_nearby);
            tabIndicatorAddress = rootView.findViewById(R.id.tab_indicator_address);
            tabIndicatorNearby = rootView.findViewById(R.id.tab_indicator_nearby);

            // Setup tab click listeners
            tabAddress.setOnClickListener(v -> switchToTab(0));
            tabNearby.setOnClickListener(v -> switchToTab(1));

            // Setup close button
            closeButton.setOnClickListener(v -> {
                if (resultsAdapter != null) resultsAdapter.clear();
                if (nearbyResultsAdapter != null) nearbyResultsAdapter.clear();
                closeDropDown();
            });

            // Setup Address Tab
            setupAddressTab();

            // Setup Nearby Tab
            setupNearbyTab();

            // Ensure correct tab is shown
            switchToTab(currentTab);

            // Show dropdown
            showDropDown(
                    rootView,
                    HALF_WIDTH,
                    FULL_HEIGHT,
                    FULL_WIDTH,
                    HALF_HEIGHT,
                    false,
                    this
            );

            // Auto-focus keyboard behavior for Address tab
            if (currentTab == 0) {
                Configuration config = pluginContext.getResources().getConfiguration();
                boolean isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT;
                boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
                int screenSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
                boolean isTablet = screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE;

                boolean shouldAutoFocus = isPortrait || (isLandscape && isTablet);
                if (shouldAutoFocus && searchInput != null) {
                    searchInput.requestFocus();
                    searchInput.postDelayed(this::showKeyboard, 200);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing search panel: " + e.getMessage(), e);
        }
    }

    private void switchToTab(int tabIndex) {
        currentTab = tabIndex;
        tabFlipper.setDisplayedChild(tabIndex);

        // Update tab appearance
        if (tabIndex == 0) {
            tabAddress.setTextColor(Color.parseColor("#00BCD4"));
            tabAddress.setTypeface(null, android.graphics.Typeface.BOLD);
            tabNearby.setTextColor(Color.parseColor("#888888"));
            tabNearby.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabIndicatorAddress.setBackgroundColor(Color.parseColor("#00BCD4"));
            tabIndicatorNearby.setBackgroundColor(Color.TRANSPARENT);
        } else {
            tabNearby.setTextColor(Color.parseColor("#00BCD4"));
            tabNearby.setTypeface(null, android.graphics.Typeface.BOLD);
            tabAddress.setTextColor(Color.parseColor("#888888"));
            tabAddress.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabIndicatorNearby.setBackgroundColor(Color.parseColor("#00BCD4"));
            tabIndicatorAddress.setBackgroundColor(Color.TRANSPARENT);
        }

        // Hide keyboard when switching tabs
        hideKeyboard();
    }

    private void setupAddressTab() {
        // Find Address tab views
            searchInput = rootView.findViewById(R.id.search_input);
            clearButton = rootView.findViewById(R.id.clear_button);
            searchStatus = rootView.findViewById(R.id.search_status);
            sectionHeader = rootView.findViewById(R.id.section_header);
            resultsRecyclerView = rootView.findViewById(R.id.search_results);
            historyContainer = rootView.findViewById(R.id.history_container);
            historyRecyclerView = rootView.findViewById(R.id.history_results);
            clearHistoryButton = rootView.findViewById(R.id.clear_history_button);
        offlineDataButton = rootView.findViewById(R.id.offline_data_button);

            // Setup search results RecyclerView
            resultsAdapter = new SearchResultsAdapter(pluginContext, this);
            resultsRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            resultsRecyclerView.setAdapter(resultsAdapter);

            // Setup history RecyclerView
            historyAdapter = new HistoryAdapter(pluginContext, this);
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            historyRecyclerView.setAdapter(historyAdapter);
            
            // Setup offline data button
            offlineDataButton.setOnClickListener(v -> {
                closeDropDown();
                Intent offlineIntent = new Intent(OfflineDataDropDown.SHOW_OFFLINE_DATA);
                AtakBroadcast.getInstance().sendBroadcast(offlineIntent);
            });

            // Setup search input
            setupSearchInput();

            // Setup buttons
        setupAddressButtons();

            // Show history if available
            refreshHistoryView();
    }

    private void setupNearbyTab() {
        // Find Nearby tab views
        selectCategoriesButton = rootView.findViewById(R.id.nearby_select_categories);
        radiusDecreaseButton = rootView.findViewById(R.id.nearby_radius_decrease);
        radiusIncreaseButton = rootView.findViewById(R.id.nearby_radius_increase);
        radiusValueText = rootView.findViewById(R.id.nearby_radius_value);
        locationSelfButton = rootView.findViewById(R.id.nearby_location_self);
        locationMapButton = rootView.findViewById(R.id.nearby_location_map);
        nearbySearchButton = rootView.findViewById(R.id.nearby_search_button);
        nearbySearchSpinner = rootView.findViewById(R.id.nearby_search_spinner);
        nearbyStatus = rootView.findViewById(R.id.nearby_status);
        nearbyResultsHeaderContainer = rootView.findViewById(R.id.nearby_results_header_container);
        nearbyResultsHeader = rootView.findViewById(R.id.nearby_results_header);
        selectAllButton = rootView.findViewById(R.id.nearby_select_all);
        deselectAllButton = rootView.findViewById(R.id.nearby_deselect_all);
        nearbyResultsRecyclerView = rootView.findViewById(R.id.nearby_results_list);
        addToMapButton = rootView.findViewById(R.id.nearby_add_to_map);
        broadcastCheckbox = rootView.findViewById(R.id.nearby_broadcast_checkbox);

        // Setup nearby results RecyclerView
        nearbyResultsAdapter = new NearbyResultsAdapter(pluginContext, this);
        nearbyResultsRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
        nearbyResultsRecyclerView.setAdapter(nearbyResultsAdapter);

        // Setup selection listener for updating the "Add to Map" button
        nearbyResultsAdapter.setSelectionListener((selectedCount, totalCount) -> {
            updateAddToMapButton(selectedCount);
        });

        // Setup "Select All" button
        selectAllButton.setOnClickListener(v -> nearbyResultsAdapter.selectAll());

        // Setup "Deselect All" button
        deselectAllButton.setOnClickListener(v -> nearbyResultsAdapter.deselectAll());

        // Setup "Add to Map" button
        addToMapButton.setOnClickListener(v -> addSelectedToMap());

        // Setup location segmented toggle
        locationSelfButton.setOnClickListener(v -> {
            useMapCenter = false;
            updateLocationToggle();
            savePreferences();
        });
        locationMapButton.setOnClickListener(v -> {
            useMapCenter = true;
            updateLocationToggle();
            savePreferences();
        });
        updateLocationToggle();

        // Setup category selection button
        selectCategoriesButton.setOnClickListener(v -> showCategorySelectionDialog());

        // Setup radius controls
        updateRadiusDisplay();
        radiusDecreaseButton.setOnClickListener(v -> {
            if (radiusIndex > 0) {
                radiusIndex--;
                updateRadiusDisplay();
                savePreferences();
            }
        });
        radiusIncreaseButton.setOnClickListener(v -> {
            if (radiusIndex < RADIUS_VALUES.length - 1) {
                radiusIndex++;
                updateRadiusDisplay();
                savePreferences();
            }
        });

        // Setup search button
        nearbySearchButton.setOnClickListener(v -> performNearbySearch());
    }

    private void showCategorySelectionDialog() {
        PointOfInterestType[] allTypes = PointOfInterestType.values();
        
        // Create a scrollable layout with checkboxes
        ScrollView scrollView = new ScrollView(getMapView().getContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        LinearLayout checkboxContainer = new LinearLayout(getMapView().getContext());
        checkboxContainer.setOrientation(LinearLayout.VERTICAL);
        checkboxContainer.setPadding(32, 16, 32, 16);
        
        // Create checkboxes for each category
        final android.widget.CheckBox[] checkBoxes = new android.widget.CheckBox[allTypes.length];
        
        for (int i = 0; i < allTypes.length; i++) {
            android.widget.CheckBox checkBox = new android.widget.CheckBox(getMapView().getContext());
            checkBox.setText(pluginContext.getString(allTypes[i].getStringResId()));
            checkBox.setChecked(selectedCategories.contains(allTypes[i]));
            checkBox.setTextColor(Color.WHITE);
            checkBox.setPadding(8, 16, 8, 16);
            checkBoxes[i] = checkBox;
            checkboxContainer.addView(checkBox);
        }
        
        scrollView.addView(checkboxContainer);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getMapView().getContext());
        builder.setTitle(pluginContext.getString(R.string.select_poi_categories));
        builder.setView(scrollView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedCategories.clear();
            for (int i = 0; i < allTypes.length; i++) {
                if (checkBoxes[i].isChecked()) {
                    selectedCategories.add(allTypes[i]);
                }
            }
            savePreferences();
        });
        builder.setNeutralButton("Clear All", (dialog, which) -> {
            selectedCategories.clear();
            savePreferences();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateRadiusDisplay() {
        radiusValueText.setText(RADIUS_VALUES[radiusIndex] + " km");
    }

    private void updateLocationToggle() {
        // Update segmented toggle appearance - use orange (#FF9800) for selected
        int selectedColor = Color.parseColor("#FF9800"); // Orange
        int unselectedTextColor = Color.parseColor("#888888");
        
        if (useMapCenter) {
            // Map selected
            locationMapButton.setTextColor(Color.WHITE);
            locationMapButton.setBackgroundColor(selectedColor);
            locationSelfButton.setTextColor(unselectedTextColor);
            locationSelfButton.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // Me/Self selected
            locationSelfButton.setTextColor(Color.WHITE);
            locationSelfButton.setBackgroundColor(selectedColor);
            locationMapButton.setTextColor(unselectedTextColor);
            locationMapButton.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void performNearbySearch() {
        if (selectedCategories.isEmpty()) {
            nearbyStatus.setText(R.string.no_categories_selected);
            nearbyStatus.setVisibility(View.VISIBLE);
            nearbyResultsHeaderContainer.setVisibility(View.GONE);
            nearbyResultsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Get search center location
        double lat, lon;
        if (useMapCenter) {
            // Use map center
            GeoPoint center = getMapView().getCenterPoint().get();
            if (center == null) {
                nearbyStatus.setText("Unable to determine map center");
                nearbyStatus.setVisibility(View.VISIBLE);
                nearbyResultsHeaderContainer.setVisibility(View.GONE);
                nearbyResultsRecyclerView.setVisibility(View.GONE);
                return;
            }
            lat = center.getLatitude();
            lon = center.getLongitude();
        } else {
            // Use self location
            Marker selfMarker = getMapView().getSelfMarker();
            if (selfMarker == null || selfMarker.getPoint() == null) {
                nearbyStatus.setText("Unable to determine your location");
                nearbyStatus.setVisibility(View.VISIBLE);
                nearbyResultsHeaderContainer.setVisibility(View.GONE);
                nearbyResultsRecyclerView.setVisibility(View.GONE);
                return;
            }
            GeoPoint selfPoint = selfMarker.getPoint();
            lat = selfPoint.getLatitude();
            lon = selfPoint.getLongitude();
        }
        int radiusKm = RADIUS_VALUES[radiusIndex];

        String locSource = useMapCenter ? "map center" : "my location";
        Log.i(TAG, "Searching nearby POIs at " + lat + ", " + lon + " (" + locSource + ") within " + radiusKm + " km");

        // Show searching status with spinner
        setNearbySearching(true);
        nearbyStatus.setText(R.string.searching_nearby);
        nearbyStatus.setVisibility(View.VISIBLE);
        nearbyResultsHeaderContainer.setVisibility(View.GONE);
        nearbyResultsRecyclerView.setVisibility(View.GONE);

        // Perform search
        List<PointOfInterestType> typesList = new ArrayList<>(selectedCategories);
        overpassClient.searchNearby(lat, lon, radiusKm, typesList, new OverpassApiClient.SearchCallback() {
            @Override
            public void onSuccess(List<OverpassSearchResult> results) {
                Log.i(TAG, "Nearby search got " + results.size() + " results");
                showNearbyResults(results);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Nearby search error: " + errorMessage);
                showNearbyError(errorMessage);
            }
        });
    }

    private void setNearbySearching(boolean searching) {
        if (searching) {
            nearbySearchButton.setText("");
            nearbySearchButton.setEnabled(false);
            nearbySearchSpinner.setVisibility(View.VISIBLE);
        } else {
            nearbySearchButton.setText(R.string.search_nearby);
            nearbySearchButton.setEnabled(true);
            nearbySearchSpinner.setVisibility(View.GONE);
        }
    }

    private void showNearbyResults(List<OverpassSearchResult> results) {
        setNearbySearching(false);
        if (results.isEmpty()) {
            nearbyStatus.setText(R.string.no_nearby_results);
            nearbyStatus.setVisibility(View.VISIBLE);
            nearbyResultsHeaderContainer.setVisibility(View.GONE);
            nearbyResultsRecyclerView.setVisibility(View.GONE);
        } else {
            nearbyStatus.setVisibility(View.GONE);
            nearbyResultsHeaderContainer.setVisibility(View.VISIBLE);
            nearbyResultsRecyclerView.setVisibility(View.VISIBLE);
            // Show result count in header
            nearbyResultsHeader.setText(String.valueOf(results.size()));
            updateAddToMapButton(0);
            nearbyResultsAdapter.setResults(results);
        }
    }

    private void showNearbyError(String message) {
        setNearbySearching(false);
        String errorText = pluginContext.getString(R.string.nearby_search_error) + ": " + message;
        nearbyStatus.setText(errorText);
        nearbyStatus.setVisibility(View.VISIBLE);
        nearbyResultsHeaderContainer.setVisibility(View.GONE);
        nearbyResultsRecyclerView.setVisibility(View.GONE);
    }

    private void updateAddToMapButton(int selectedCount) {
        if (selectedCount > 0) {
            addToMapButton.setText(pluginContext.getString(R.string.add_to_map) + " (" + selectedCount + ")");
        } else {
            addToMapButton.setText(R.string.add_to_map);
        }
    }

    private void addSelectedToMap() {
        List<OverpassSearchResult> selected = nearbyResultsAdapter.getSelectedResults();
        if (selected.isEmpty()) {
            // If nothing selected, show message
            android.widget.Toast.makeText(pluginContext, 
                R.string.no_results_selected, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user wants to broadcast to TAK network
        boolean shouldBroadcast = broadcastCheckbox != null && broadcastCheckbox.isChecked();
        
        // Add markers with broadcast option
        addAllSelectedWithAutoType(selected, shouldBroadcast);
    }

    /**
     * Check if custom icons preference is enabled.
     */
    private boolean isCustomIconsEnabled() {
        SharedPreferences prefs = android.preference.PreferenceManager
            .getDefaultSharedPreferences(getMapView().getContext());
        return prefs.getBoolean("nearby_use_custom_icons", true);
    }

    // Batch size for adding markers (to prevent UI thread overload)
    private static final int MARKER_BATCH_SIZE = 25;
    private static final long MARKER_BATCH_DELAY_MS = 50;

    private void addAllSelectedWithAutoType(List<OverpassSearchResult> results, boolean broadcastToNetwork) {
        MapGroup rootGroup = getMapView().getRootGroup();
        if (rootGroup == null) {
            Log.e(TAG, "Could not find root map group");
            return;
        }

        MapGroup userObjects = rootGroup.findMapGroup("User Objects");
        if (userObjects == null) {
            userObjects = rootGroup.addGroup("User Objects");
        }

        final MapGroup finalUserObjects = userObjects;
        final boolean useCustomIcons = isCustomIconsEnabled();
        final int totalCount = results.size();
        
        // Show progress for large batches
        if (totalCount > MARKER_BATCH_SIZE) {
            android.widget.Toast.makeText(pluginContext, 
                "Adding " + totalCount + " markers...", 
                android.widget.Toast.LENGTH_SHORT).show();
        }
        
        // Clear selection immediately for better UX
        nearbyResultsAdapter.deselectAll();
        updateAddToMapButton(0);
        
        // Process markers in batches on a background thread
        new Thread(() -> {
            int successCount = 0;
            int skippedCount = 0;
            
            for (int i = 0; i < results.size(); i++) {
                final OverpassSearchResult result = results.get(i);
                final int index = i;
                
                try {
                    // Generate consistent UID based on OSM ID to prevent duplicates
                    String uid = generatePoiUid(result);
                    
                    // Check if marker already exists
                    if (getMapView().getRootGroup().deepFindUID(uid) != null) {
                        Log.d(TAG, "Marker already exists, skipping: " + uid);
                        skippedCount++;
                        continue;
                    }
                    
                    // Create marker (can be done on background thread)
                    Marker marker = createMarkerForResult(result, useCustomIcons, uid);
                    
                    // Add to map on UI thread
                    mainHandler.post(() -> {
                        try {
                            finalUserObjects.addItem(marker);
                            
                            // Only refresh every few markers to reduce UI load
                            if (index % 10 == 0 || index == results.size() - 1) {
                                marker.refresh(getMapView().getMapEventDispatcher(), null, AddressSearchDropDown.class);
                            }
                            
                            // Broadcast if requested (throttled)
                            if (broadcastToNetwork) {
                                broadcastMarker(marker);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding marker to map: " + e.getMessage());
                        }
                    });
                    
                    successCount++;
                    
                    // Yield between batches to prevent overwhelming the system
                    if ((i + 1) % MARKER_BATCH_SIZE == 0 && i < results.size() - 1) {
                        try {
                            Thread.sleep(MARKER_BATCH_DELAY_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error creating marker: " + e.getMessage());
                }
            }
            
            // Show completion toast on UI thread
            final int finalCount = successCount;
            final int finalSkipped = skippedCount;
            mainHandler.post(() -> {
                Log.i(TAG, "Added " + finalCount + " POI markers to map" + (broadcastToNetwork ? " (broadcasted)" : "") + 
                      (finalSkipped > 0 ? ", skipped " + finalSkipped + " duplicates" : ""));
                String message;
                if (finalSkipped > 0 && finalCount == 0) {
                    message = "All " + finalSkipped + " markers already on map";
                } else if (finalSkipped > 0) {
                    message = "Added " + finalCount + " markers (" + finalSkipped + " already on map)";
                } else if (broadcastToNetwork) {
                    message = String.format(pluginContext.getString(R.string.added_markers_broadcast), finalCount);
                } else {
                    message = String.format(pluginContext.getString(R.string.added_markers), finalCount);
                }
                android.widget.Toast.makeText(pluginContext, message, android.widget.Toast.LENGTH_SHORT).show();
            });
            
        }, "AddMarkersThread").start();
    }
    
    /**
     * Generate a consistent UID for a POI based on its OSM ID.
     * This prevents duplicate markers when the same POI is added multiple times.
     */
    private String generatePoiUid(OverpassSearchResult result) {
        // Use OSM type and ID for a unique, consistent identifier
        return "poi-" + result.getOsmType() + "-" + result.getOsmId();
    }
    
    /**
     * Create a marker for a POI result (can be called from background thread).
     */
    private Marker createMarkerForResult(OverpassSearchResult result, boolean useCustomIcons, String uid) {
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
            PointOfInterestType poiType = result.getPoiType();

            Marker marker = new Marker(uid);
            marker.setPoint(point);
            marker.setTitle(result.getDisplayName());
            marker.setMovable(false);
            marker.setEditable(false);
            
            // Check preference - if custom icons disabled, just use CoT type
            String internalIconUri = null;
            if (useCustomIcons && poiType != null) {
                internalIconUri = iconsetHelper.getInternalIconUri(poiType);
            }
            
            if (internalIconUri != null) {
                // Use ATAK's built-in internal icon - set ALL icon metadata fields for persistence
                marker.setMetaString("iconUri", internalIconUri);
                marker.setMetaString("usericon", internalIconUri);
                marker.setMetaString("iconsetPath", internalIconUri);  // lowercase version
                marker.setMetaString("IconsetPath", internalIconUri);  // uppercase version
                marker.setType(poiType.getCotType());
            } else if (useCustomIcons) {
                // Fall back to iconset approach
                com.atakmap.android.icons.UserIcon atakIcon = null;
                if (poiType != null) {
                    atakIcon = iconsetHelper.getAtakIcon(poiType);
                }
                
                String markerType = "a-u-G";
                if (atakIcon != null && atakIcon.get2525cType() != null) {
                    markerType = atakIcon.get2525cType();
                } else if (poiType != null) {
                    markerType = poiType.getCotType();
                }
                marker.setType(markerType);
                
                if (atakIcon != null) {
                    String iconsetPath = atakIcon.getIconsetPath();
                    if (iconsetPath != null && !iconsetPath.isEmpty()) {
                        // Set ALL icon metadata fields for persistence after marker edit
                        marker.setMetaString("IconsetPath", iconsetPath);
                        marker.setMetaString("iconsetPath", iconsetPath);  // lowercase version
                        marker.setMetaString("usericon", iconsetPath);
                        marker.setMetaString("iconUri", iconsetPath);
                    }
                }
            } else {
                String markerType = (poiType != null) ? poiType.getCotType() : "a-u-G";
                marker.setType(markerType);
            }
            
            // Build remarks with POI info
            StringBuilder remarks = new StringBuilder();
            if (poiType != null) {
                remarks.append(pluginContext.getString(poiType.getStringResId()));
            }
            if (result.getAddress() != null && !result.getAddress().isEmpty()) {
                if (remarks.length() > 0) remarks.append("\n");
                remarks.append(result.getAddress());
            }
            marker.setMetaString("remarks", remarks.toString());
            
            marker.setMetaBoolean("readiness", true);
            marker.setMetaBoolean("archive", true);
            marker.setMetaString("how", "h-g-i-g-o");
            marker.setMetaString("entry", "user");

        return marker;
    }
    
    /**
     * Broadcast a marker to the TAK network.
     * Uses the same approach as RIDAR - dispatch to both internal and external dispatchers.
     */
    private void broadcastMarker(Marker marker) {
        try {
            // Persist locally first
            marker.persist(getMapView().getMapEventDispatcher(), null, AddressSearchDropDown.class);
            
            // Build CotEvent manually from marker properties (like RIDAR does)
            CotEvent cotEvent = createCotEventFromMarker(marker);
            if (cotEvent != null && cotEvent.isValid()) {
                CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
                CotMapComponent.getExternalDispatcher().dispatch(cotEvent);
                Log.d(TAG, "Broadcasted marker to internal + external: " + marker.getUID());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting marker: " + e.getMessage());
        }
    }
    
    /**
     * Create a CotEvent from a Marker (similar to how RIDAR generates CoT events).
     */
    private CotEvent createCotEventFromMarker(Marker marker) {
        try {
            CotEvent event = new CotEvent();
            
            // Set UID and type
            event.setUID(marker.getUID());
            event.setType(marker.getType());
            
            // Set times
            com.atakmap.coremap.maps.time.CoordinatedTime now = new com.atakmap.coremap.maps.time.CoordinatedTime();
            com.atakmap.coremap.maps.time.CoordinatedTime stale = new com.atakmap.coremap.maps.time.CoordinatedTime(
                now.getMilliseconds() + (60 * 60 * 1000)); // 1 hour stale
            event.setTime(now);
            event.setStart(now);
            event.setStale(stale);
            event.setHow("h-g-i-g-o");
            
            // Set point
            GeoPoint point = marker.getPoint();
            com.atakmap.coremap.cot.event.CotPoint cotPoint = new com.atakmap.coremap.cot.event.CotPoint(
                point.getLatitude(), point.getLongitude(), point.getAltitude(),
                com.atakmap.coremap.cot.event.CotPoint.UNKNOWN, 
                com.atakmap.coremap.cot.event.CotPoint.UNKNOWN);
            event.setPoint(cotPoint);
            
            // Build detail
            com.atakmap.coremap.cot.event.CotDetail detail = new com.atakmap.coremap.cot.event.CotDetail("detail");
            
            // Add contact (callsign/title)
            com.atakmap.coremap.cot.event.CotDetail contact = new com.atakmap.coremap.cot.event.CotDetail("contact");
            contact.setAttribute("callsign", marker.getTitle() != null ? marker.getTitle() : marker.getUID());
            detail.addChild(contact);
            
            // Add remarks
            String remarks = marker.getMetaString("remarks", "");
            if (!remarks.isEmpty()) {
                com.atakmap.coremap.cot.event.CotDetail remarksDetail = new com.atakmap.coremap.cot.event.CotDetail("remarks");
                remarksDetail.setInnerText(remarks);
                detail.addChild(remarksDetail);
            }
            
            // Add usericon if present
            String usericon = marker.getMetaString("usericon", null);
            if (usericon != null) {
                com.atakmap.coremap.cot.event.CotDetail useridDetail = new com.atakmap.coremap.cot.event.CotDetail("usericon");
                useridDetail.setAttribute("iconsetpath", usericon);
                detail.addChild(useridDetail);
            }
            
            // Add status
            com.atakmap.coremap.cot.event.CotDetail status = new com.atakmap.coremap.cot.event.CotDetail("status");
            status.setAttribute("readiness", "true");
            detail.addChild(status);
            
            event.setDetail(detail);
            
            return event;
        } catch (Exception e) {
            Log.e(TAG, "Error creating CotEvent from marker: " + e.getMessage());
            return null;
        }
    }

    // NearbyResultsAdapter.OnResultClickListener implementation
    @Override
    public void onResultClick(OverpassSearchResult result) {
        navigateToPoi(result);
    }

    @Override
    public void onNavigateClick(OverpassSearchResult result) {
        startBloodhoundNavigationToPoi(result);
    }

    @Override
    public void onDropMarkerClick(OverpassSearchResult result) {
        dropMarkerAtPoi(result);
    }

    private void navigateToPoi(OverpassSearchResult result) {
        Log.i(TAG, "Navigating to POI: " + result.getDisplayName());
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        getMapView().getMapController().panTo(point, true);
        getMapView().getMapController().zoomTo(17.0, true);
    }

    private void startBloodhoundNavigationToPoi(OverpassSearchResult result) {
        Log.i(TAG, "Starting Bloodhound navigation to POI: " + result.getDisplayName());

        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        String uid = "nearby-nav-" + UUID.randomUUID().toString().substring(0, 8);

        Marker marker = new Marker(point, uid);
        marker.setType("b-m-p-w-GOTO");
        marker.setTitle(result.getDisplayName());
        marker.setMetaString("remarks", result.getAddress());
        marker.setMetaBoolean("readiness", true);
        marker.setMetaBoolean("archive", true);
        marker.setMetaString("how", "h-g-i-g-o");

        MapGroup rootGroup = getMapView().getRootGroup();
        if (rootGroup != null) {
            MapGroup userObjects = rootGroup.findMapGroup("User Objects");
            if (userObjects == null) {
                userObjects = rootGroup.addGroup("User Objects");
            }
            userObjects.addItem(marker);
            
            navigateToPoi(result);

            Intent bloodhoundIntent = new Intent();
            bloodhoundIntent.setAction(BloodHoundTool.BLOOD_HOUND);
            bloodhoundIntent.putExtra("uid", uid);
            AtakBroadcast.getInstance().sendBroadcast(bloodhoundIntent);
        }
    }

    /**
     * Convert a Bitmap to a data URI string for use with ATAK Icon.Builder
     */
    private String bitmapToDataUri(Bitmap bitmap) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        return "base64://" + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }

    private void dropMarkerAtPoi(OverpassSearchResult result) {
        Log.i(TAG, "Dropping marker at POI: " + result.getDisplayName());

        PointOfInterestType poiType = result.getPoiType();
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        String uid = "nearby-marker-" + UUID.randomUUID().toString().substring(0, 8);

        Marker marker = new Marker(uid);
        marker.setPoint(point);
        marker.setTitle(result.getDisplayName());
        marker.setMovable(false);
        marker.setEditable(false);
        
        boolean useCustomIcons = isCustomIconsEnabled();
        
        // Check for ATAK internal icons (if custom icons enabled)
        String internalIconUri = null;
        if (useCustomIcons && poiType != null) {
            internalIconUri = iconsetHelper.getInternalIconUri(poiType);
        }
        
        if (internalIconUri != null) {
            // Use ATAK's built-in internal icon - set ALL icon metadata fields for persistence
            Log.i(TAG, "Using ATAK internal icon for " + poiType.name() + ": " + internalIconUri);
            marker.setMetaString("iconUri", internalIconUri);
            marker.setMetaString("usericon", internalIconUri);
            marker.setMetaString("iconsetPath", internalIconUri);  // lowercase version
            marker.setMetaString("IconsetPath", internalIconUri);  // uppercase version
            marker.setType(poiType.getCotType());
        } else if (useCustomIcons) {
            // Fall back to iconset approach (same as Nearby plugin)
            com.atakmap.android.icons.UserIcon atakIcon = null;
            if (poiType != null) {
                atakIcon = iconsetHelper.getAtakIcon(poiType);
            }
            
            // Get CoT type from icon
            String markerType = "a-u-G";
            if (atakIcon != null && atakIcon.get2525cType() != null) {
                markerType = atakIcon.get2525cType();
            } else if (poiType != null) {
                markerType = poiType.getCotType();
            }
            marker.setType(markerType);
            
            // Set ALL icon metadata fields for persistence after marker edit
            if (atakIcon != null) {
                String iconsetPath = atakIcon.getIconsetPath();
                if (iconsetPath != null && !iconsetPath.isEmpty()) {
                    marker.setMetaString("IconsetPath", iconsetPath);
                    marker.setMetaString("iconsetPath", iconsetPath);  // lowercase version
                    marker.setMetaString("usericon", iconsetPath);
                    marker.setMetaString("iconUri", iconsetPath);
                }
            }
        } else {
            // Custom icons disabled - use CoT type only
            String markerType = (poiType != null) ? poiType.getCotType() : "a-u-G";
            marker.setType(markerType);
        }
        
        // Build remarks with POI info
        StringBuilder remarks = new StringBuilder();
        if (poiType != null) {
            remarks.append(pluginContext.getString(poiType.getStringResId()));
        }
        if (result.getAddress() != null && !result.getAddress().isEmpty()) {
            if (remarks.length() > 0) remarks.append("\n");
            remarks.append(result.getAddress());
        }
        marker.setMetaString("remarks", remarks.toString());
        
        marker.setMetaBoolean("readiness", true);
        marker.setMetaBoolean("archive", true);
        marker.setMetaString("how", "h-g-i-g-o");

        MapGroup rootGroup = getMapView().getRootGroup();
        if (rootGroup != null) {
            MapGroup userObjects = rootGroup.findMapGroup("User Objects");
            if (userObjects == null) {
                userObjects = rootGroup.addGroup("User Objects");
            }
            userObjects.addItem(marker);
            
            // Refresh the marker to ensure icon is applied
            marker.refresh(getMapView().getMapEventDispatcher(), null, AddressSearchDropDown.class);
            
            navigateToPoi(result);
            
            android.widget.Toast.makeText(pluginContext, 
                "Marker added: " + result.getDisplayName(), 
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSearchInput() {
        // Text change listener with debouncing
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s != null ? s.toString().trim() : "";
                pendingQuery = text;

                // Show/hide clear button
                clearButton.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);

                // Cancel any pending search
                mainHandler.removeCallbacks(searchRunnable);

                if (text.length() >= MIN_QUERY_LENGTH) {
                    // Hide history when searching
                    historyContainer.setVisibility(View.GONE);
                    // Debounce: wait before searching
                    mainHandler.postDelayed(searchRunnable, DEBOUNCE_DELAY_MS);
                } else {
                    // Show history when not searching
                    showIdle();
                    refreshHistoryView();
                }
            }
        });

        // Handle Enter key for immediate search
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                mainHandler.removeCallbacks(searchRunnable);
                String query = searchInput.getText().toString().trim();
                if (query.length() >= MIN_QUERY_LENGTH) {
                    performSearch(query);
                }
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupAddressButtons() {
        // Clear button
        clearButton.setOnClickListener(v -> {
            searchInput.setText("");
            resultsAdapter.clear();
            showIdle();
            refreshHistoryView();
        });

        // Clear history button
        clearHistoryButton.setOnClickListener(v -> {
            historyManager.clearHistory();
            refreshHistoryView();
        });
    }

    private void refreshHistoryView() {
        List<NominatimSearchResult> history = historyManager.getHistory();
        if (history.isEmpty()) {
            historyContainer.setVisibility(View.GONE);
        } else {
            historyAdapter.setItems(history);
            historyContainer.setVisibility(View.VISIBLE);
        }
    }

    // Default radius for category search (km)
    private static final int CATEGORY_SEARCH_RADIUS_KM = 10;

    private void performSearch(String query) {
        Log.i(TAG, "Searching for: " + query);
        
        // Check if this is a category/POI search (e.g., "gas station near me")
        CategoryMatcher.MatchResult categoryMatch = CategoryMatcher.detectCategory(query);
        
        if (categoryMatch.hasMatch()) {
            // It's a POI category search - perform nearby search
            Log.i(TAG, "Detected POI category search: " + categoryMatch.getCategory().name() + 
                       " (nearby=" + categoryMatch.isNearbyQuery() + ")");
            performCategorySearch(categoryMatch);
            return;
        }
        
        // If query contains "near me/nearby" but no category matched, show helpful message
        if (categoryMatch.isNearbyQuery()) {
            Log.i(TAG, "Nearby query detected but no category matched: " + query);
        showSearching();
            // Still try address search - it might be a place name like "starbucks near me"
        } else {
            showSearching();
        }

        // Standard address search
        apiClient.search(query, new NominatimApiClient.SearchCallback() {
            @Override
            public void onSuccess(List<NominatimSearchResult> results) {
                Log.i(TAG, "Got " + results.size() + " results");
                showResults(results);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Search error: " + errorMessage);
                showError(errorMessage);
            }
        });
    }
    
    /**
     * Perform a POI category search based on the matched category.
     * Uses the user's current location as the search center.
     */
    private void performCategorySearch(CategoryMatcher.MatchResult match) {
        showSearching();
        
        // Get user's location (self marker)
        Marker selfMarker = getMapView().getSelfMarker();
        double lat, lon;
        
        if (selfMarker != null && selfMarker.getPoint() != null) {
            GeoPoint selfPoint = selfMarker.getPoint();
            lat = selfPoint.getLatitude();
            lon = selfPoint.getLongitude();
        } else {
            // Fall back to map center if self location unavailable
            GeoPoint center = getMapView().getCenterPoint().get();
            if (center != null) {
                lat = center.getLatitude();
                lon = center.getLongitude();
                Log.i(TAG, "Using map center for category search (self location unavailable)");
            } else {
                showError(pluginContext.getString(R.string.unable_to_determine_location));
                return;
            }
        }
        
        PointOfInterestType category = match.getCategory();
        String categoryName = CategoryMatcher.getCategoryDisplayName(category);
        
        Log.i(TAG, "Performing category search for " + categoryName + " at " + lat + ", " + lon);
        
        // Update status to show what we're searching for
        String searchingMsg = pluginContext.getString(R.string.searching_for_category, categoryName);
        searchStatus.setText(searchingMsg);
        searchStatus.setVisibility(View.VISIBLE);
        
        // Perform the POI search
        List<PointOfInterestType> types = Collections.singletonList(category);
        overpassClient.searchNearby(lat, lon, CATEGORY_SEARCH_RADIUS_KM, types, 
            new OverpassApiClient.SearchCallback() {
                @Override
                public void onSuccess(List<OverpassSearchResult> results) {
                    Log.i(TAG, "Category search got " + results.size() + " results for " + categoryName);
                    showCategoryResults(results, category);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Category search error: " + errorMessage);
                    showError(errorMessage);
                }
            });
    }
    
    /**
     * Display POI search results in the Address tab's results area.
     */
    private void showCategoryResults(List<OverpassSearchResult> poiResults, PointOfInterestType category) {
        historyContainer.setVisibility(View.GONE);
        
        if (poiResults.isEmpty()) {
            String categoryName = CategoryMatcher.getCategoryDisplayName(category);
            String noResultsMsg = pluginContext.getString(R.string.no_category_results, categoryName);
            searchStatus.setText(noResultsMsg);
            searchStatus.setVisibility(View.VISIBLE);
            sectionHeader.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            searchStatus.setVisibility(View.GONE);
            sectionHeader.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
            // Use the adapter's POI mode to display results with distance
            resultsAdapter.setPoiResults(poiResults, pluginContext);
        }
    }

    private void showIdle() {
        searchStatus.setVisibility(View.GONE);
        sectionHeader.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.GONE);
    }

    private void showSearching() {
        historyContainer.setVisibility(View.GONE);
        searchStatus.setText(R.string.searching);
        searchStatus.setVisibility(View.VISIBLE);
        sectionHeader.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.GONE);
    }

    private void showResults(List<NominatimSearchResult> results) {
        historyContainer.setVisibility(View.GONE);
        if (results.isEmpty()) {
            searchStatus.setText(R.string.no_results);
            searchStatus.setVisibility(View.VISIBLE);
            sectionHeader.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            searchStatus.setVisibility(View.GONE);
            sectionHeader.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
            resultsAdapter.setResults(results);
        }
    }

    private void showError(String message) {
        String errorText = pluginContext.getString(R.string.search_error) + ": " + message;
        searchStatus.setText(errorText);
        searchStatus.setVisibility(View.VISIBLE);
        sectionHeader.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onResultClick(NominatimSearchResult result) {
        navigateToResult(result);
        historyManager.addToHistory(result);
        searchInput.setText("");
        resultsAdapter.clear();
        showIdle();
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemClick(NominatimSearchResult result) {
        navigateToResult(result);
        historyManager.addToHistory(result);
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemRemove(NominatimSearchResult result) {
        historyManager.removeFromHistory(result.getPlaceId());
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemNavigate(NominatimSearchResult result) {
        startBloodhoundNavigation(result);
    }

    @Override
    public void onHistoryItemDropMarker(NominatimSearchResult result) {
        dropMarkerAtLocation(result);
    }

    @Override
    public void onResultDropMarker(NominatimSearchResult result) {
        dropMarkerAtLocation(result);
        historyManager.addToHistory(result);
    }

    @Override
    public void onResultNavigate(NominatimSearchResult result) {
        startBloodhoundNavigation(result);
        historyManager.addToHistory(result);
    }

    private void startBloodhoundNavigation(NominatimSearchResult result) {
        Log.i(TAG, "Starting Bloodhound navigation to: " + result.getName());
        
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        String uid = "address-nav-" + UUID.randomUUID().toString().substring(0, 8);
        
        Marker marker = new Marker(point, uid);
        marker.setType("b-m-p-w-GOTO");
        marker.setTitle(result.getName());
        marker.setMetaString("remarks", result.getDisplayName());
        marker.setMetaBoolean("readiness", true);
        marker.setMetaBoolean("archive", true);
        marker.setMetaString("how", "h-g-i-g-o");
        
        MapGroup rootGroup = getMapView().getRootGroup();
        if (rootGroup != null) {
            MapGroup userObjects = rootGroup.findMapGroup("User Objects");
            if (userObjects == null) {
                userObjects = rootGroup.addGroup("User Objects");
            }
            userObjects.addItem(marker);
            Log.d(TAG, "Navigation marker dropped: " + result.getName());
            
            navigateToResult(result);
            
            Intent bloodhoundIntent = new Intent();
            bloodhoundIntent.setAction(BloodHoundTool.BLOOD_HOUND);
            bloodhoundIntent.putExtra("uid", uid);
            AtakBroadcast.getInstance().sendBroadcast(bloodhoundIntent);
            Log.d(TAG, "Bloodhound started for: " + uid);
        } else {
            Log.e(TAG, "Could not find root map group");
        }
    }

    /**
     * Marker type options with CoT type codes and MIL-STD-2525 colors/shapes.
     */
    private static class MarkerType {
        final String name;
        final String cotType;
        final int color;
        final int shapeType;
        
        MarkerType(String name, String cotType, int color, int shapeType) {
            this.name = name;
            this.cotType = cotType;
            this.color = color;
            this.shapeType = shapeType;
        }
    }
    
    private static final int COLOR_FRIENDLY = Color.rgb(128, 224, 255);
    private static final int COLOR_HOSTILE = Color.rgb(255, 128, 128);
    private static final int COLOR_NEUTRAL = Color.rgb(170, 255, 170);
    private static final int COLOR_UNKNOWN = Color.rgb(255, 255, 128);
    
    private static final MarkerType[] MARKER_TYPES = {
        new MarkerType("Friendly", "a-f-G", COLOR_FRIENDLY, 0),
        new MarkerType("Hostile", "a-h-G", COLOR_HOSTILE, 1),
        new MarkerType("Neutral", "a-n-G", COLOR_NEUTRAL, 2),
        new MarkerType("Unknown", "a-u-G", COLOR_UNKNOWN, 3),
    };
    
    private Bitmap createMarkerIcon(int color, int shapeType, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(color);
        fillPaint.setStyle(Paint.Style.FILL);
        
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3);
        
        int padding = 4;
        int w = size - padding * 2;
        int h = size - padding * 2;
        int cx = size / 2;
        int cy = size / 2;
        
        switch (shapeType) {
            case 0:
                canvas.drawRoundRect(padding, padding + h/6, size - padding, size - padding - h/6, 8, 8, fillPaint);
                canvas.drawRoundRect(padding, padding + h/6, size - padding, size - padding - h/6, 8, 8, strokePaint);
                break;
                
            case 1:
                Path diamond = new Path();
                diamond.moveTo(cx, padding);
                diamond.lineTo(size - padding, cy);
                diamond.lineTo(cx, size - padding);
                diamond.lineTo(padding, cy);
                diamond.close();
                canvas.drawPath(diamond, fillPaint);
                canvas.drawPath(diamond, strokePaint);
                break;
                
            case 2:
                canvas.drawRect(padding, padding, size - padding, size - padding, fillPaint);
                canvas.drawRect(padding, padding, size - padding, size - padding, strokePaint);
                break;
                
            case 3:
                float radius = w / 4.5f;
                canvas.drawCircle(cx, cy - radius, radius, fillPaint);
                canvas.drawCircle(cx, cy + radius, radius, fillPaint);
                canvas.drawCircle(cx - radius, cy, radius, fillPaint);
                canvas.drawCircle(cx + radius, cy, radius, fillPaint);
                canvas.drawCircle(cx, cy - radius, radius, strokePaint);
                canvas.drawCircle(cx, cy + radius, radius, strokePaint);
                canvas.drawCircle(cx - radius, cy, radius, strokePaint);
                canvas.drawCircle(cx + radius, cy, radius, strokePaint);
                break;
        }
        
        return bitmap;
    }
    
    private void dropMarkerAtLocation(NominatimSearchResult result) {
        Log.i(TAG, "Showing marker type dialog for: " + result.getName());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getMapView().getContext());
        builder.setTitle("Marker Type");
        
        LinearLayout layout = new LinearLayout(getMapView().getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(16, 16, 16, 16);
        
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        int iconSize = 56;
        
        for (MarkerType markerType : MARKER_TYPES) {
            Bitmap iconBitmap = createMarkerIcon(markerType.color, markerType.shapeType, iconSize);
            
            android.widget.ImageView iconView = new android.widget.ImageView(getMapView().getContext());
            iconView.setImageBitmap(iconBitmap);
            iconView.setClickable(true);
            iconView.setFocusable(true);
            iconView.setBackgroundResource(android.R.drawable.list_selector_background);
            iconView.setPadding(12, 12, 12, 12);
            
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize + 24, iconSize + 24);
            iconParams.setMargins(8, 0, 8, 0);
            iconView.setLayoutParams(iconParams);
            
            final String cotType = markerType.cotType;
            iconView.setOnClickListener(v -> {
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
                createMarkerWithType(result, cotType);
            });
            
            layout.addView(iconView);
        }
        
        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        dialog.show();
    }
    
    private void createMarkerWithType(NominatimSearchResult result, String cotType) {
        Log.i(TAG, "Dropping marker at: " + result.getName() + " with type: " + cotType);
        
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        String uid = "address-marker-" + UUID.randomUUID().toString().substring(0, 8);
        
        Marker marker = new Marker(point, uid);
        marker.setType(cotType);
        marker.setTitle(result.getName());
        marker.setMetaString("remarks", result.getDisplayName());
        marker.setMetaBoolean("readiness", true);
        marker.setMetaBoolean("archive", true);
        marker.setMetaString("how", "h-g-i-g-o");
        
        MapGroup rootGroup = getMapView().getRootGroup();
        if (rootGroup != null) {
            MapGroup userObjects = rootGroup.findMapGroup("User Objects");
            if (userObjects == null) {
                userObjects = rootGroup.addGroup("User Objects");
            }
            userObjects.addItem(marker);
            Log.d(TAG, "Marker dropped: " + result.getName() + " (" + cotType + ")");
            
            navigateToResult(result);
        } else {
            Log.e(TAG, "Could not find root map group");
        }
    }

    private void navigateToResult(NominatimSearchResult result) {
        Log.i(TAG, "Navigating to: " + result.getName() + " at " +
                result.getLatitude() + ", " + result.getLongitude());

        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        double zoomLevel = result.getZoomLevel();

        getMapView().getMapController().panTo(point, true);
        getMapView().getMapController().zoomTo(zoomLevel, true);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) pluginContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchInput != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) pluginContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchInput != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
        cleanup();
    }

    @Override
    public void onDropDownClose() {
        cleanup();
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {}

    @Override
    public void onDropDownVisible(boolean visible) {}

    private void cleanup() {
        mainHandler.removeCallbacks(searchRunnable);
        if (resultsAdapter != null) {
            resultsAdapter.clear();
        }
        if (nearbyResultsAdapter != null) {
            nearbyResultsAdapter.clear();
        }
    }

    @Override
    protected void disposeImpl() {
        cleanup();
        apiClient.shutdown();
        overpassClient.shutdown();
    }
}
