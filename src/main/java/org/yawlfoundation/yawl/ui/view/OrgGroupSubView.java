package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.resourcing.resource.OrgGroup;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.dialog.orgdata.OrgGroupDialog;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class OrgGroupSubView extends AbstractOrgDataView<OrgGroup> {

    public OrgGroupSubView() {
        super();
    }


    @Override
    List<OrgGroup> getItems() {
        List<OrgGroup> groups = new ArrayList<>();
        try {
            getResourceClient().getOrgGroups().forEach(o -> groups.add((OrgGroup) o));
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of OrgGroups from engine : %s",
                      e.getMessage());
         }
         return groups;
    }


    @Override
    String getTitle() {
        return "Org Groups";
    }


    @Override
    void addColumns(Grid<OrgGroup> grid) {
        super.addColumns(grid);
        grid.addColumn(OrgGroup::getGroupType).setHeader(UiUtil.bold("Group Type"));
    }


    @Override
    void addBelongsToColumn(Grid<OrgGroup> grid) {
        grid.addColumn(this::getBelongsToName).setHeader(UiUtil.bold("Belongs To"));
    }


    @Override
    protected boolean hasMembers() {
        return false;
    }

    
    @Override
    List<Participant> getMembers(OrgGroup item) {
        // org groups don't have member participants - this is never called
        return null;
    }

    @Override
    void addMembers(List<Participant> pList, OrgGroup item) { }


    @Override
    void removeMembers(List<Participant> pList, OrgGroup item) { }


    @Override
    AbstractOrgDataDialog<OrgGroup> createDialog(List<OrgGroup> existingItems, OrgGroup item) {
        return new OrgGroupDialog(existingItems, item, getAllParticipants(), null,
                getBelongsTo(item));
    }

    @Override
    OrgGroup addItem(OrgGroup item) {
        try {
            return getResourceClient().addOrgGroup(item);
        }
        catch (IOException e) {
            Announcement.error("Failed to add Org Group : %s", e.getMessage());
        }
        return item;
    }


    @Override
    boolean updateItem(OrgGroup item) {
        try {
            return successful(getResourceClient().updateOrgGroup(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to update Org Group : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean removeItem(OrgGroup item) {
        try {
            return successful(getResourceClient().removeOrgGroup(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to remove Org Group : %s", e.getMessage());
        }
        return false;
    }


    protected OrgGroup getBelongsTo(OrgGroup og) {
        if (og != null) {
            String btid = og.get_belongsToID();
            if (btid != null) {
                try {
                    return getResourceClient().getOrgGroup(btid);
                }
                catch (IOException | ResourceGatewayException e) {
                    //fall through;
                }
            }
        }
        return null;
    }


    protected String getBelongsToName(OrgGroup og) {
        OrgGroup belongsTo = getBelongsTo(og);
        return belongsTo != null ? belongsTo.getName() : "";
    }

}
