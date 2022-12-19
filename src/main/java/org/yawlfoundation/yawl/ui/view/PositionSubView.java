package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.resourcing.resource.OrgGroup;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Position;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.dialog.orgdata.PositionDialog;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class PositionSubView extends AbstractOrgDataView<Position> {

    public PositionSubView() {
        super();
    }


    @Override
    List<Position> getItems() {
        List<Position> positions = new ArrayList<>();
        try {
            getResourceClient().getPositions().forEach(p -> positions.add((Position) p));
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Positions from engine : %s",
                      e.getMessage());
         }
         return positions;
    }


    @Override
    String getTitle() {
        return "Positions";
    }


    @Override
    void addColumns(Grid<Position> grid) {
        super.addColumns(grid);
        grid.addColumn(this::getOrgGroupName).setHeader(UiUtil.bold("Org Group"));
    }


    @Override
    void addBelongsToColumn(Grid<Position> grid) {
        grid.addColumn(this::getReportsToName).setHeader(UiUtil.bold("Reports To"));
    }


    @Override
    List<Participant> getMembers(Position item) {
        if (item != null) {
            try {
                return getResourceClient().getPositionMembers(item.getName());
            }
            catch (IOException | ResourceGatewayException e) {
                Announcement.warn(
                        "Failed to retrieve list of Position members from engine : %s",
                        e.getMessage());
            }
        }
        return Collections.emptyList();
    }


    @Override
    void addMembers(List<Participant> pList, Position item) {
        pList.forEach(p -> {
            try {
                getResourceClient().addParticipantToPosition(p.getID(), item.getID());
            }
            catch (IOException e) {
                Announcement.error("Failed to add % to Position : %s",
                        p.getFullName(), e.getMessage());
            }
        });
    }


    @Override
    void removeMembers(List<Participant> pList, Position item) {
        pList.forEach(p -> {
        try {
            getResourceClient().removeParticipantFromPosition(p.getID(), item.getID());
        }
        catch (IOException e) {
            Announcement.error("Failed to remove % from Position : %s",
                    p.getFullName(), e.getMessage());
        }
        });
    }


    @Override
    AbstractOrgDataDialog<Position> createDialog(List<Position> existingItems, Position item) {
        return new PositionDialog(existingItems, item, getAllParticipants(), getMembers(item),
                getReportsTo(item), getAllOrgGroups(), getOrgGroup(item));
    }

    @Override
    Position addItem(Position item) {
        try {
            return getResourceClient().addPosition(item);
        }
        catch (IOException e) {
            Announcement.error("Failed to add Position : %s", e.getMessage());
        }
        return item;
    }


    @Override
    boolean updateItem(Position item) {
        try {
            return successful(getResourceClient().updatePosition(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to update Position : %s", e.getMessage());
        }
        return false;
    }


    @Override
    boolean removeItem(Position item) {
        try {
            return successful(getResourceClient().removePosition(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to remove Position : %s", e.getMessage());
        }
        return false;
    }


    protected String getOrgGroupName(Position p) {
        OrgGroup group = getOrgGroup(p);
        return group != null ? group.getName() : "";
    }


    protected OrgGroup getOrgGroup(Position p) {
        if (p != null) {
            String oid = p.get_orgGroupID();
            if (oid != null) {
                try {
                    return getResourceClient().getOrgGroup(oid);
                }
                catch (IOException | ResourceGatewayException e) {
                    //fall through;
                }
            }
        }
        return null;
    }


    protected String getReportsToName(Position p) {
        Position reportsTo = getReportsTo(p);
        return reportsTo != null ? reportsTo.getName() : "";
    }


    protected Position getReportsTo(Position p) {
        if (p != null) {
            String rtid = p.get_reportsToID();
            if (rtid != null) {
                try {
                    return getResourceClient().getPosition(rtid);
                }
                catch (IOException | ResourceGatewayException e) {
                    //fall through;
                }
            }
        }
        return null;
    }


    private List<OrgGroup> getAllOrgGroups() {
        List<OrgGroup> groups = new ArrayList<>();
        try {
           getResourceClient().getOrgGroups().forEach(o -> groups.add((OrgGroup) o));
        }
        catch (IOException | ResourceGatewayException e) {
            // fall through;
        }
        return groups;
    }

}
