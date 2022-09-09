package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.resourcing.resource.OrgGroup;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Position;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
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
public class PositionSubView extends AbstractOrgDataView<Position> {

    public PositionSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
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
        try {
            return getResourceClient().getPositionMembers(item.getName());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                     "Failed to retrieve list of Position members from engine : %s",
                      e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    AbstractOrgDataDialog<Position> createDialog(List<Position> existingItems, Position item) {
        return null;
    }

    @Override
    boolean addItem(Position item) {
        try {
            return successful(getResourceClient().addPosition(item));
        }
        catch (IOException e) {
            Announcement.error("Failed to add Position : %s", e.getMessage());
        }
        return false;
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
        String oid = p.get_orgGroupID();
        if (oid != null) {
            try {
                OrgGroup og = getResourceClient().getOrgGroup(oid);
                if (og != null) {
                    return og.getName();
                }
            }
            catch (IOException | ResourceGatewayException e) {
                //fall through;
            }
        }
        return "";
    }


    protected String getReportsToName(Position p) {
        String rtid = p.get_reportsToID();
        if (rtid != null) {
            try {
                Position rPos = getResourceClient().getPosition(rtid);
                if (rPos != null) {
                    return rPos.getName();
                }
            }
            catch (IOException | ResourceGatewayException e) {
                //fall through;
            }
        }
        return "";
    }

}
