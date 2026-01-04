package com.gotak.address.search;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.dropdown.DropDown;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.gotak.address.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.List;

/**
 * DropDown receiver for the address search panel.
 * Provides location search using Nominatim (OpenStreetMap) API.
 */
public class AddressSearchDropDown extends DropDownReceiver implements
        DropDown.OnStateListener,
        SearchResultsAdapter.OnResultClickListener,
        HistoryAdapter.HistoryItemListener {

    public static final String TAG = "AddressSearchDropDown";
    public static final String SHOW_SEARCH = "com.gotak.address.SHOW_ADDRESS_SEARCH";
    public static final String HIDE_SEARCH = "com.gotak.address.HIDE_ADDRESS_SEARCH";

    private static final long DEBOUNCE_DELAY_MS = 300;
    private static final int MIN_QUERY_LENGTH = 2;

    private final Context pluginContext;
    private final NominatimApiClient apiClient;
    private final Handler mainHandler;
    private final SearchHistoryManager historyManager;

    // UI elements
    private View rootView;
    private EditText searchInput;
    private ImageButton clearButton;
    private ImageButton closeButton;
    private TextView searchStatus;
    private TextView sectionHeader;
    private RecyclerView resultsRecyclerView;
    private SearchResultsAdapter resultsAdapter;

    // History UI elements
    private LinearLayout historyContainer;
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private TextView clearHistoryButton;

    // Debounce handling
    private final Runnable searchRunnable;
    private String pendingQuery = "";

    public AddressSearchDropDown(MapView mapView, Context pluginContext) {
        super(mapView);
        this.pluginContext = pluginContext;
        this.apiClient = new NominatimApiClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.historyManager = new SearchHistoryManager(pluginContext);

        // Create debounced search runnable
        this.searchRunnable = () -> {
            if (pendingQuery.length() >= MIN_QUERY_LENGTH) {
                performSearch(pendingQuery);
            }
        };
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

            // Find views
            searchInput = rootView.findViewById(R.id.search_input);
            clearButton = rootView.findViewById(R.id.clear_button);
            closeButton = rootView.findViewById(R.id.close_button);
            searchStatus = rootView.findViewById(R.id.search_status);
            sectionHeader = rootView.findViewById(R.id.section_header);
            resultsRecyclerView = rootView.findViewById(R.id.search_results);

            // History views
            historyContainer = rootView.findViewById(R.id.history_container);
            historyRecyclerView = rootView.findViewById(R.id.history_results);
            clearHistoryButton = rootView.findViewById(R.id.clear_history_button);

            // Setup search results RecyclerView
            resultsAdapter = new SearchResultsAdapter(pluginContext, this);
            resultsRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            resultsRecyclerView.setAdapter(resultsAdapter);

            // Setup history RecyclerView
            historyAdapter = new HistoryAdapter(pluginContext, this);
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(pluginContext));
            historyRecyclerView.setAdapter(historyAdapter);

            // Setup search input
            setupSearchInput();

            // Setup buttons
            setupButtons();

            // Show history if available
            refreshHistoryView();

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

            // Auto-focus keyboard behavior:
            // - Portrait mode (any device): auto-focus and show keyboard
            // - Tablet in landscape: auto-focus and show keyboard
            // - Phone in landscape: do NOT auto-focus (keyboard takes too much space)
            Configuration config = pluginContext.getResources().getConfiguration();
            boolean isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT;
            boolean isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
            int screenSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            boolean isTablet = screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE;
            boolean isPhone = !isTablet;

            // Only auto-focus if: portrait mode, OR tablet in landscape
            // Do NOT auto-focus: phone in landscape (let user tap to focus)
            boolean shouldAutoFocus = isPortrait || (isLandscape && isTablet);

            if (shouldAutoFocus) {
                searchInput.requestFocus();
                searchInput.postDelayed(this::showKeyboard, 200);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing search panel: " + e.getMessage(), e);
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

    private void setupButtons() {
        // Clear button
        clearButton.setOnClickListener(v -> {
            searchInput.setText("");
            resultsAdapter.clear();
            showIdle();
            refreshHistoryView();
        });

        // Close button
        closeButton.setOnClickListener(v -> {
            resultsAdapter.clear();
            closeDropDown();
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

    private void performSearch(String query) {
        Log.i(TAG, "Searching for: " + query);
        showSearching();

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
        // Add to history when selected from search results
        historyManager.addToHistory(result);
        // Keep pane open - clear search and show updated history
        searchInput.setText("");
        resultsAdapter.clear();
        showIdle();
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemClick(NominatimSearchResult result) {
        navigateToResult(result);
        // Move to top of history when clicked
        historyManager.addToHistory(result);
        // Keep pane open - refresh history to show updated order
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemRemove(NominatimSearchResult result) {
        historyManager.removeFromHistory(result.getPlaceId());
        refreshHistoryView();
    }

    @Override
    public void onHistoryItemNavigate(NominatimSearchResult result) {
        Log.i(TAG, "Opening Google Maps for: " + result.getDisplayName());
        
        // Create Google Maps navigation intent using address
        // Format: google.navigation:q=address
        String encodedAddress = Uri.encode(result.getDisplayName());
        String uri = "google.navigation:q=" + encodedAddress;
        
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            pluginContext.startActivity(mapIntent);
        } catch (Exception e) {
            // Google Maps not installed, try generic geo intent with address
            Log.w(TAG, "Google Maps not available, trying generic geo intent");
            String geoUri = "geo:0,0?q=" + encodedAddress;
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                pluginContext.startActivity(geoIntent);
            } catch (Exception e2) {
                Log.e(TAG, "No map application available", e2);
            }
        }
    }

    private void navigateToResult(NominatimSearchResult result) {
        Log.i(TAG, "Navigating to: " + result.getName() + " at " +
                result.getLatitude() + ", " + result.getLongitude());

        // Create GeoPoint and navigate
        GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
        double zoomLevel = result.getZoomLevel();

        // Pan and zoom to the selected location
        getMapView().getMapController().panTo(point, true);
        getMapView().getMapController().zoomTo(zoomLevel, true);

        // Don't close the panel - let caller handle UI updates
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
    }

    @Override
    protected void disposeImpl() {
        cleanup();
        apiClient.shutdown();
    }
}
