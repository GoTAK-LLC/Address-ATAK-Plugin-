
package com.gotak.address.tools;

import android.os.Bundle;

import com.atakmap.android.lrf.LocalRangeFinderInput;
import com.atakmap.android.lrf.RangeFinderAction;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.toolbar.Tool;
import com.atakmap.android.toolbar.ToolManagerBroadcastReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;

import gov.tak.api.annotation.NonNull;

/**
 * Example tool used to show
 */
public class LRFPointTool extends Tool implements RangeFinderAction {

    private static final String TAG = "LRFPointTool";
    public static final String TOOL_IDENTIFIER = "com.gotak.address.tool.LRFPointTool";

    private final LRFCallback callback;


    public interface LRFCallback {
        void onResults(double distance, double azimuth, double inclination, boolean success);
    }

    public LRFPointTool(@NonNull MapView mapView, @NonNull LRFCallback callback) {
        super(mapView, TOOL_IDENTIFIER);
        this.callback = callback;
        ToolManagerBroadcastReceiver.getInstance().registerTool(
                TOOL_IDENTIFIER, this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean onToolBegin(Bundle extras) {
        TextContainer.getInstance().displayPrompt("Fire Laser Range Finder");
        // the bundle could provide information as to which button is being used, etc.
        LocalRangeFinderInput.getInstance().registerAction(this);
        return true;
    }

    @Override
    public void onToolEnd() {
        LocalRangeFinderInput.getInstance().registerAction(null);
        TextContainer.getInstance().closePrompt();
        super.onToolEnd();
    }

    @Override
    public void onRangeFinderInfo(String uidPrefix, final double distance,
            final double azimuth, final double inclination) {
        if (callback != null) {
            callback.onResults(distance, azimuth, inclination, true);
        }
        requestEndTool();
    }

}
