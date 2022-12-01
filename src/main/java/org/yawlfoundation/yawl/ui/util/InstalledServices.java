package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 29/11/2022
 */
public class InstalledServices {

    private final List<YAWLServiceReference> _services;


    public InstalledServices(ResourceClient resClient) {
        _services = getServices(resClient);
    }


    public boolean hasWorkletService() {
        for (YAWLServiceReference service : _services) {
            if (service.getURI().contains("workletService")) {
                return true;
            }
        }
        return false;
    }

    private List<YAWLServiceReference> getServices(ResourceClient resClient) {
        try {
            return resClient.getRegisteredServices();
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }

}
