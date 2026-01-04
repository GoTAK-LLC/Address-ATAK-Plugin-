
package com.gotak.address;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.gotak.address.plugin.R;
import com.gotak.address.search.AddressSearchDropDown;
import com.gotak.address.search.SearchButtonWidget;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.graphics.GLMapItemFactory;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

/**
 * This is an example of a MapComponent within the ATAK 
 * ecosphere.   A map component is the building block for all
 * activities within the system.   This defines a concrete 
 * thought or idea. 
 */
public class AddressMapComponent extends DropDownMapComponent {

    public static final String TAG = "AddressMapComponent";

    private Context pluginContext;
    private AddressMapOverlay mapOverlay;
    private SpecialDetailHandler sdh;
    private CotDetailHandler aaaDetailHandler;

    // Address search components
    private SearchButtonWidget searchButtonWidget;
    private AddressSearchDropDown addressSearchDropDown;
    
    // Self location geocoding widget
    private com.gotak.address.selfgeo.SelfLocationWidget selfLocationWidget;

    @Override
    public void onStart(final Context context, final MapView view) {
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause(final Context context, final MapView view) {
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(final Context context,
            final MapView view) {
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop(final Context context,
            final MapView view) {
        Log.d(TAG, "onStop");
    }

    @Override
    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        // Set the theme.  Otherwise, the plugin will look vastly different
        // than the main ATAK experience.   The theme needs to be set 
        // programatically because the AndroidManifest.xml is not used.
        context.setTheme(R.style.ATAKPluginTheme);

        super.onCreate(context, intent, view);
        pluginContext = context;

        GLMapItemFactory.registerSpi(GLSpecialMarker.SPI);

        // Register capability to handle detail tags that TAK does not 
        // normally process.
        CotDetailManager.getInstance().registerHandler(
                "__special",
                sdh = new SpecialDetailHandler());

        CotDetailManager.getInstance().registerHandler(
                aaaDetailHandler = new CotDetailHandler("__aaa") {
                    private final String TAG = "AAACotDetailHandler";

                    @Override
                    public CommsMapComponent.ImportResult toItemMetadata(
                            MapItem item, CotEvent event, CotDetail detail) {
                        Log.d(TAG, "detail received: " + detail + " in:  "
                                + event);
                        return CommsMapComponent.ImportResult.SUCCESS;
                    }

                    @Override
                    public boolean toCotDetail(MapItem item, CotEvent event,
                            CotDetail root) {
                        Log.d(TAG, "converting to cot detail from: "
                                + item.getUID());
                        return true;
                    }
                });

        //Address MapOverlay added to Overlay Manager.
        this.mapOverlay = new AddressMapOverlay(view, pluginContext);
        view.getMapOverlayManager().addOverlay(this.mapOverlay);

        //MapView.getMapView().getRootGroup().getChildGroupById(id).setVisible(true);

        /*Intent new_cot_intent = new Intent();
        new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
        new_cot_intent.putExtra("uid", point.getUID());
        AtakBroadcast.getInstance().sendBroadcast(
                new_cot_intent);*/

        // Initialize Address Search components
        try {
            Log.d(TAG, "Initializing address search components");
            
            // Create and register the search dropdown receiver
            addressSearchDropDown = new AddressSearchDropDown(view, pluginContext);
            DocumentedIntentFilter searchFilter = new DocumentedIntentFilter();
            searchFilter.addAction(AddressSearchDropDown.SHOW_SEARCH,
                    "Show the address search panel");
            searchFilter.addAction(AddressSearchDropDown.HIDE_SEARCH,
                    "Hide the address search panel");
            this.registerDropDownReceiver(addressSearchDropDown, searchFilter);
            
            // Create the search button widget
            searchButtonWidget = new SearchButtonWidget();
            searchButtonWidget.onCreate(context, intent, view);
            
            // Create the self location widget (shows address above callsign)
            selfLocationWidget = new com.gotak.address.selfgeo.SelfLocationWidget();
            selfLocationWidget.onCreate(pluginContext, intent, view);
            
            Log.d(TAG, "Address search components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize address search components: " + e.getMessage(), e);
        }

    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "calling on destroy");
        
        // Clean up address search components
        try {
            if (searchButtonWidget != null) {
                searchButtonWidget.dispose();
                searchButtonWidget = null;
            }
            if (addressSearchDropDown != null) {
                addressSearchDropDown.dispose();
                addressSearchDropDown = null;
            }
            if (selfLocationWidget != null) {
                selfLocationWidget.dispose();
                selfLocationWidget = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up address search components: " + e.getMessage(), e);
        }
        
        GLMapItemFactory.unregisterSpi(GLSpecialMarker.SPI);
        view.getMapOverlayManager().removeOverlay(mapOverlay);
        CotDetailManager.getInstance().unregisterHandler(sdh);
        CotDetailManager.getInstance().unregisterHandler(aaaDetailHandler);
        
        super.onDestroyImpl(context, view);
    }
}
