
package com.gotak.address.navstack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.gotak.address.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigationstack.DropDownNavigationStack;
import com.atakmap.android.navigationstack.NavigationStackItem;

/**
 * A NavigationStackItem that, in this case, serves as the root view
 * for the DropDownNavigationStack.
 */
public class NavigationStackDropDown extends NavigationStackItem {

    private final Context pluginContext;

    public static final String TAG = "NavigationStackDropdown";

    public NavigationStackDropDown(MapView mapView, View itemView,
            Context context) {
        super(mapView);
        _itemView = itemView;
        pluginContext = context;

        Button toolbar = itemView.findViewById(R.id.toolbarBtn);

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addStackItem();
            }
        });
    }

    @Override
    public String getTitle() {
        return "Dropdown Root View";
    }

    private void addStackItem() {
        /* Here we construct another item to be pushed onto the stack */
        LayoutInflater inflater = LayoutInflater.from(pluginContext);
        View itemView = inflater
                .inflate(R.layout.navigation_stack_dropdown_child, null);
        NavigationStackItem backItem = new NavigationStackDropdownChild(
                getMapView(), itemView, pluginContext);

        /* use the current nav stack */
        DropDownNavigationStack navStack = getNavigationStack();
        backItem.setNavigationStack(navStack);
        navStack.pushView(backItem);
    }
}
