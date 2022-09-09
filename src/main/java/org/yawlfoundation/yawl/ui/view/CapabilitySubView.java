package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.resourcing.resource.Capability;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class CapabilitySubView extends AbstractOrgDataView<Capability> {

    public CapabilitySubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
    }


    @Override
    List<Capability> getItems() {
        List<Capability> capabilities = new ArrayList<>();
        try {
            getResourceClient().getCapabilities()
                    .forEach(c -> capabilities.add((Capability) c));
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Capabilities from engine : %s",
                      e.getMessage());
         }
         return capabilities;
    }


    @Override
    String getTitle() {
        return "Capabilities";
    }


    @Override
    void addBelongsToColumn(Grid<Capability> grid) {
        // no belongs relation for capabilities
    }

    @Override
    AbstractOrgDataDialog<Capability> createDialog(List<Capability> existingItems, Capability item) {
        return null;
    }



    @Override
    List<Participant> getMembers(Capability item) {
        try {
            return getResourceClient().getCapabilityMembers(item.getName());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Capability members from engine : %s",
                      e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    boolean addItem(Capability item) {
        try {
            return successful(getResourceClient().addCapability(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to add Capability : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean updateItem(Capability item) {
        try {
            return successful(getResourceClient().updateCapability(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to update Capability : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean removeItem(Capability item) {
        try {
            return successful(getResourceClient().removeCapability(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to remove Capability : %s", e.getMessage());
        }
        return false;
    }
    
}
