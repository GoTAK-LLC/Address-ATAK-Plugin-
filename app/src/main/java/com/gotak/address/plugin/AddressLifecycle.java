
package com.gotak.address.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import gov.tak.api.plugin.IServiceController;
import com.gotak.address.AddressMapComponent;

public class AddressLifecycle extends AbstractPlugin {

    public AddressLifecycle(IServiceController serviceController) {
        super(serviceController, new AddressTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new AddressMapComponent());
    }
}
