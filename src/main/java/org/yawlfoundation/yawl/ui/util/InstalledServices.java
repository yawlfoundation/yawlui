package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.ui.service.Clients;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 29/11/2022
 */
public class InstalledServices {

    private final List<YAWLServiceReference> _services;


    public InstalledServices() {
        _services = getServices();
    }


    public boolean hasWorkletService() {
        for (YAWLServiceReference service : _services) {
            if (service.getURI().contains("workletService")) {
                return true;
            }
        }
        return false;
    }


    private List<YAWLServiceReference> getServices() {
        try {
            return Clients.getResourceClient().getRegisteredServices();
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }

}
