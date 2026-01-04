
package com.gotak.address.navstack;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.gotak.address.plugin.R;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.navigationstack.NavigationStackItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A NavStackItem with a list of buttons for the toolbar. In this case,
 * the item is added to an existing NavStack in the NavigationStackDropdown.
 */
class NavigationStackDropdownChild extends NavigationStackItem {
    private final Context pluginContext;

    Button _hide;
    Button _defaultButtonSet;
    Button _altButtonSet;
    String _mode = "default";

    public NavigationStackDropdownChild(MapView mapView, View itemView,
            Context context) {
        super(mapView);
        _itemView = itemView;
        pluginContext = context;

        _hide = _itemView.findViewById(R.id.hide);
        _hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideView();
            }
        });

        _defaultButtonSet = _itemView.findViewById(R.id.defaultButtons);
        _defaultButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                defaultMode();
            }
        });

        _altButtonSet = _itemView.findViewById(R.id.altButtons);
        _altButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alternateMode();
            }
        });
    }

    private void defaultMode() {
        _mode = "default";
        updateButtons();
    }

    private void alternateMode() {
        _mode = "alt";
        updateButtons();
    }

    /**
     * A set of buttons for the default view mode.
     */
    private List<ImageButton> getDefaultButtons() {
        ArrayList<ImageButton> buttons = new ArrayList<>();

        /* Use the createButton() convenience methods to create compatible toolbar buttons */
        ImageButton backButton = createButton(
                com.atakmap.app.R.drawable.back_navigation,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onBackButton();
                    }
                });

        ImageButton addButton = createButton(com.atakmap.app.R.drawable.ic_add,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(pluginContext,
                                pluginContext.getText(R.string.perform_add),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        ImageButton actionButton = createButton(
                com.atakmap.app.R.drawable.icon_edit,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(pluginContext,
                                pluginContext.getText(R.string.perform_edit),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        buttons.add(backButton);
        buttons.add(addButton);
        buttons.add(actionButton);

        return buttons;
    }

    /**
     * A set of buttons for an alternate view mode.
     */
    private List<ImageButton> getAltButtons() {
        ArrayList<ImageButton> buttons = new ArrayList<>();

        /* Use the createButton() convenience methods to create compatible toolbar buttons */
        ImageButton backButton = createButton(
                com.atakmap.app.R.drawable.back_navigation,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        defaultMode();
                    }
                });

        ImageButton multiSelectButton = createButton(
                com.atakmap.app.R.drawable.ic_track_multiselect,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(pluginContext,
                                pluginContext
                                        .getText(R.string.perform_multiselect),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        buttons.add(backButton);
        buttons.add(multiSelectButton);

        return buttons;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /* NavigationStackItem is a BroadcastReceiver so we can listen for broadcasts here */
    }

    @Override
    public boolean onBackButton() {
        /* we only want to pop the view, so return true to keep the dropdown open.*/
        popView();
        return true;
    }

    @Override
    public String getTitle() {
        return "DropDown Child View";
    }

    @Override
    public List<ImageButton> getButtons() {
        /* get the proper button set when updateButtons() is called */
        if (_mode.equals("default")) {
            return getDefaultButtons();
        } else {
            return getAltButtons();
        }
    }
}
