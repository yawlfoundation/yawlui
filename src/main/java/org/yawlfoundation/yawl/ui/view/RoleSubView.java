package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Role;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.dialog.orgdata.RoleDialog;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class RoleSubView extends AbstractOrgDataView<Role> {

    public RoleSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
    }


    @Override
    List<Role> getItems() {
        List<Role> roles = new ArrayList<>();
        try {
            getResourceClient().getRoles().forEach(r -> roles.add((Role) r));
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Roles from engine : %s",
                      e.getMessage());
         }
         return roles;
    }


    @Override
    String getTitle() {
        return "Roles";
    }


    @Override
    void addBelongsToColumn(Grid<Role> grid) {
        grid.addColumn(this::getBelongsToName).setHeader(UiUtil.bold("Belongs To"));
    }

    @Override
    AbstractOrgDataDialog<Role> createDialog(List<Role> existingItems, Role item) {
        return new RoleDialog(existingItems, item, getAllParticipants(),
                getMembers(item), getBelongsTo(item));
    }

    @Override
    List<Participant> getMembers(Role item) {
        try {
            return getResourceClient().getRoleMembers(item.getName());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Role members from engine : %s",
                      e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    boolean addItem(Role item) {
        try {
            return successful(getResourceClient().addRole(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to add Role : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean updateItem(Role item) {
        try {
            return successful(getResourceClient().updateRole(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to update Role : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean removeItem(Role item) {
        try {
            return successful(getResourceClient().removeRole(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to remove Role : %s", e.getMessage());
        }
        return false;
    }


    protected String getBelongsToName(Role r) {
        Role btRole = getBelongsTo(r);
        return btRole != null ? btRole.getName() : "";
    }


    protected Role getBelongsTo(Role r) {
        String btid = r.get_belongsToID();
        if (btid != null) {
            try {
                return getResourceClient().getRole(btid);
            }
            catch (IOException | ResourceGatewayException e) {
                //fall through;
            }
        }
        return null;
    }
    
}
