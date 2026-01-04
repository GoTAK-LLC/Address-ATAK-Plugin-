
package com.gotak.address.speechtotext;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.routes.Route;
import com.atakmap.android.routes.RouteMapReceiver;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes in String addresses and starts navigation to them.
 */
public class SpeechNavigator extends SpeechActivity {
    private static final String TAG = "SPEECH_NAVIGATOR";

    private String geoAddress;
    private GeoPoint destination;
    private GeoPoint source;
    private Route route;

    private String inputDestination;
    private String inputOrigin;
    private final boolean navFlag;

    public SpeechNavigator(MapView mapview, String input, Boolean quickNav) {
        super(mapview, mapview.getContext());
        this.navFlag = quickNav;
        analyzeSpeech(input);
    }

    /**
     * Gets the string addresses for destination and origin
     * Example: Navigate from taco bell to planned parenthood
     * It gets the index of from and to, then gets the words between them.
     * Putting them in their respective Strings.
     * @param input - The speech input
     */
    @Override
    void analyzeSpeech(String input) {
        int toIndex = -1;
        int fromIndex = -1;
        StringBuilder destinationBuilder = new StringBuilder();
        String[] inputArr = input.split(" ");
        for (int i = 0; i < inputArr.length; i++) {
            if (inputArr[i].equalsIgnoreCase("to"))
                toIndex = i;
            if (inputArr[i].equalsIgnoreCase("from"))
                fromIndex = i;
        }
        //Now get whats after from and to
        if (toIndex != -1) {
            for (int i = toIndex + 1; i < inputArr.length; i++) {
                destinationBuilder.append(inputArr[i]);
                destinationBuilder.append(" ");
            }
            inputDestination = destinationBuilder.toString().trim();
        }
        if (fromIndex != -1) {
            StringBuilder origin = new StringBuilder();
            if (fromIndex > toIndex) {
                for (int i = fromIndex + 1; i < inputArr.length; i++) {
                    origin.append(inputArr[i]);
                    origin.append(" ");
                }
                inputOrigin = origin.toString().trim();
            } else {
                for (int i = fromIndex + 1; i < toIndex; i++) {
                    origin.append(inputArr[i]);
                    origin.append(" ");
                }
            }
            inputOrigin = origin.toString().trim();
        }
        startActivity();
    }

    /**
     * Uses the destination and source to plot a route.
     * VNS kicks in if it's installed.
     * If quickNav is true, it sends an intent with the routeID
     * starting navigation mode automatically
     */
    @Override
    void startActivity() {
        if (inputOrigin != null)
            originFinder(inputOrigin);
        else
            source = getView().getSelfMarker().getPoint();
        GeoBounds gb = getView().getBounds();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Address> addressList =
                        GeocodeManager.getInstance(getView().getContext()).getSelectedGeocoder().getLocation(inputDestination, gb);
                GeoPoint gp = null;
                if (!FileSystemUtils.isEmpty(addressList)) {
                    gp = new GeoPoint(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                }

                if (gp != null) {
                    Log.d(TAG, "Inside GeocodingTask result listener");
                    destination = gp;
                    if (!navFlag) {
                        route = RouteMapReceiver.promptPlanRoute(getView(),
                                source, destination,
                                "Route to " + inputDestination, Color.RED);
                        route.persist(getView().getMapEventDispatcher(), null,
                                this.getClass());
                    } else {
                        route = RouteMapReceiver.promptPlanRoute(getView(),
                                source, destination,
                                "Route to " + inputDestination, Color.RED);
                        route.persist(getView().getMapEventDispatcher(), null,
                                this.getClass());
                        Intent startNavIntent = new Intent(
                                RouteMapReceiver.START_NAV)
                                .putExtra("routeUID", route.getUID());
                        AtakBroadcast.getInstance()
                                .sendBroadcast(startNavIntent);
                    }
                } else {
                    getView().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getPluginContext(),
                                    "Address not found, Try moving map",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        t.start();
    }

    /**
     * If the origin is also an address, it finds the geopoint
     * Navigates from x to y : it finds x
     * @param s - the address to find
     */
    private void originFinder(String s) {
        GeoBounds gb = getView().getBounds();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                List<Address> addressList =
                        GeocodeManager.getInstance(getView().getContext()).getSelectedGeocoder().getLocation(inputDestination, gb);
                GeoPoint gp = null;
                if (!FileSystemUtils.isEmpty(addressList)) {
                    gp = new GeoPoint(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                }

                if (gp != null) {
                    Log.d(TAG, "Inside GeocodingTask result listener");
                    source = gp;
                } else {
                    getView().post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getPluginContext(),
                                    "Origin Address not found, Try moving map",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
        t.start();
    }

}
