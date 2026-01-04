package com.gotak.address.search;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.AbstractWidgetMapComponent;
import com.atakmap.android.widgets.LinearLayoutWidget;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.RootLayoutWidget;
import com.atakmap.android.widgets.TextWidget;
import com.atakmap.coremap.log.Log;

/**
 * Map widget component that adds a "Search" button to the bottom-left of the map.
 * When clicked, it broadcasts an intent to show the address search dropdown.
 */
public class SearchButtonWidget extends AbstractWidgetMapComponent
        implements MapWidget.OnClickListener {

    private static final String TAG = "SearchButtonWidget";
    private static final int FONT_SIZE = 2;
    private static final float HORIZONTAL_MARGIN = 12f;
    private static final float VERTICAL_MARGIN = 8f;

    private LinearLayoutWidget layout;
    private TextWidget widget;

    @Override
    protected void onCreateWidgets(Context context, Intent intent, MapView mapView) {
        Log.v(TAG, "onCreateWidgets");

        try {
            // Get root layout and find bottom-left container
            Object rootObj = mapView.getComponentExtra("rootLayoutWidget");
            if (rootObj == null) {
                Log.e(TAG, "Could not find rootLayoutWidget");
                return;
            }
            
            RootLayoutWidget root = (RootLayoutWidget) rootObj;

            // Get or create layout in bottom-left
            LinearLayoutWidget bottomLeft = root.getLayout(RootLayoutWidget.BOTTOM_LEFT);
            if (bottomLeft == null) {
                Log.e(TAG, "Could not find BOTTOM_LEFT layout");
                return;
            }
            
            layout = bottomLeft.getOrCreateLayout("AddressSearch_H");
            if (layout == null) {
                Log.e(TAG, "Could not create AddressSearch_H layout");
                return;
            }

            // Create "Search" text widget
            widget = new TextWidget("Search", FONT_SIZE);
            widget.setName("AddressSearchButton");
            widget.setMargins(HORIZONTAL_MARGIN, VERTICAL_MARGIN, 0f, VERTICAL_MARGIN);
            widget.setColor(0xFFFFFFFF); // White
            widget.addOnClickListener(this);

            // Add to layout (at position 0 to be first)
            layout.addChildWidgetAt(0, widget);

            Log.d(TAG, "Search button added to map");
        } catch (Exception e) {
            Log.e(TAG, "Error creating search button widget: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroyWidgets(Context context, MapView mapView) {
        Log.v(TAG, "onDestroyWidgets");
        cleanup();
    }

    /**
     * Public method to clean up the widget.
     * Call this when the plugin is being destroyed.
     */
    public void dispose() {
        cleanup();
    }

    private void cleanup() {
        if (layout != null && widget != null) {
            layout.removeWidget(widget);
        }
        widget = null;
        layout = null;
    }

    @Override
    public void onMapWidgetClick(MapWidget widget, MotionEvent event) {
        Log.d(TAG, "Search button clicked");

        // Broadcast intent to show the search dropdown
        AtakBroadcast.getInstance().sendBroadcast(
                new Intent(AddressSearchDropDown.SHOW_SEARCH)
        );
    }

    @Override
    public void onStart(Context context, MapView view) {
        // No-op
    }

    @Override
    public void onStop(Context context, MapView view) {
        // No-op
    }

    @Override
    public void onPause(Context context, MapView view) {
        // No-op
    }

    @Override
    public void onResume(Context context, MapView view) {
        // No-op
    }
}

