
package com.gotak.address.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.gotak.address.search.AddressSearchDropDown;
import gov.tak.api.util.Disposable;

/**
 * Plugin toolbar tool that opens the address search when tapped.
 */
public class AddressTool extends AbstractPluginTool implements Disposable {

    private final static String TAG = "AddressTool";

    public AddressTool(final Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher, null),
                AddressSearchDropDown.SHOW_SEARCH);
    }

    @Override
    public void dispose() {
        // Nothing to clean up
    }
}
