
package com.gotak.address.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.plugin.IServiceController;
import com.gotak.address.AddressMapComponent;

public class AddressLifecycle extends AbstractPlugin {

    public AddressLifecycle(IServiceController serviceController) {
        // No toolbar icon - search is triggered via the search button widget on the map
        super(serviceController, (AbstractPluginTool) null, new AddressMapComponent());
    }
}
