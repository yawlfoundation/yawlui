package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class AdminWorklistView extends AbstractWorklistView {

    public AdminWorklistView(ResourceClient client) {
        super(client);
    }


    @Override
    protected QueueSet getQueueSet() {
        try {
            return getClient().getAdminWorkQueues();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return new QueueSet(null, QueueSet.setType.adminSet, false);
        }
    }


    @Override
    protected String getTitle() {
        return "Active Work Items";
    }


    @Override
    protected ActionRibbon createColumnActions(WorkItemRecord wir) {
        ActionRibbon ribbon = new ActionRibbon();
        switch(wir.getResourceStatus()) {
            case "Unoffered" : {
                ribbon.add(VaadinIcon.HANDSHAKE, "#0066FF", "Offer",
                        event -> offerItem(wir));
                ribbon.add(VaadinIcon.INBOX, "#0066FF", "Allocate",
                        event -> allocateItem(wir));
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Start",
                        event -> startItem(wir));
                break;
            }

            // the rest fall through deliberately
            // todo ribbon insert empty icons to keep aligned (or maybe align right)
            // todo standardise colours with ActionRibbon constants
            case "Started" :
            case "Suspended" :
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Restart",
                    event -> restartItem(wir));
            case "Allocated" :
                ribbon.add(0, VaadinIcon.INBOX, "#0066FF", "Reallocate",
                    event -> reallocateItem(wir));
            case "Offered" :
                ribbon.add(0, VaadinIcon.HANDSHAKE, "#0066FF", "Reoffer",
                        event -> reofferItem(wir));
        }
        ribbon.add(VaadinIcon.COG_O, "#0066FF", "Settings",
                event -> settings());

        return ribbon;
    }


    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "#0066FF", "Refresh", event ->
                refreshGrid());

        ribbon.add(VaadinIcon.CLOSE_SMALL, "red", "Unload", event -> {
             Announcement.highlight("click");
        });
        return ribbon;
    }


    @Override
    protected Grid<WorkItemRecord> createGrid() {
        Grid<WorkItemRecord> grid = super.createGrid();
        grid.addComponentColumn(this::getAssignedParticipants).setAutoWidth(true)
                .setResizable(true).setHeader(UiUtil.bold("Assigned"));

        // reorder columns
        List<Grid.Column<WorkItemRecord>> columns = new ArrayList<>(grid.getColumns());
        Grid.Column<WorkItemRecord> pColumn = columns.remove(columns.size() -1);
        columns.add(columns.size() -1, pColumn);
        grid.setColumnOrder(columns);
        return grid;
    }


    private ParticipantCombo getAssignedParticipants(WorkItemRecord wir) {
        return new ParticipantCombo(wir);
    }

    //todo next 2 methods: dialog for selecting participants (with filtering)
    private Set<String> getSelectedParticipantIDs() {
        return Collections.emptySet();
    }


    private String getSelectedParticipantID() {
        return "";
    }


    private void offerItem(WorkItemRecord wir) {
        try {
            _resClient.offerItem(wir.getID(), getSelectedParticipantIDs());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void allocateItem(WorkItemRecord wir) {
        try {
            _resClient.allocateItem(wir.getID(), getSelectedParticipantID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void startItem(WorkItemRecord wir) {
        try {
            _resClient.startItem(wir.getID(), getSelectedParticipantID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    //TODO Add next three to resclient
    private void reofferItem(WorkItemRecord wir) {
        try {
            _resClient.offerItem(wir.getID(), getSelectedParticipantIDs());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void reallocateItem(WorkItemRecord wir) {
        try {
            _resClient.allocateItem(wir.getID(), getSelectedParticipantID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void restartItem(WorkItemRecord wir) {
        try {
            _resClient.startItem(wir.getID(), getSelectedParticipantID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void settings() {
        //todo dialog for 'directly to me'
    }

    class ParticipantCombo extends ComboBox<Participant>
            implements Comparable<ParticipantCombo> {

        ParticipantCombo(WorkItemRecord wir) {
            int queue = -1;
            switch (wir.getResourceStatus()) {
                case "Offered" : queue = WorkQueue.OFFERED; break;
                case "Allocated" : queue = WorkQueue.ALLOCATED; break;
                case "Started" : queue = WorkQueue.STARTED; break;
                case "Suspended" : queue = WorkQueue.SUSPENDED; break;
            }
            if (queue > -1) {
                try {
                    List<Participant> pList = _resClient.getAssignedParticipants(
                            wir.getID(), queue).stream()
                            .sorted(Comparator.comparing(Participant::getLastName)
                                    .thenComparing(Participant::getFirstName))
                            .collect(Collectors.toList());
                    setItems(pList);
                    setItemLabelGenerator(Participant::getFullName);
                    if (! pList.isEmpty()) setValue(pList.get(0));
                }
                catch (IOException |ResourceGatewayException e) {
                    // leave it empty
                }
            }
            
        }

        @Override
        public int compareTo(ParticipantCombo other) {
            if (getValue() == null) {
                return 1;
            }
            if (other.getValue() == null) {
                return -1;
            }
            int comp = getValue().getLastName().compareTo(other.getValue().getLastName());
            if (comp == 0) {
                return getValue().getFirstName().compareTo((other.getValue().getFirstName()));
            }
            return comp;
        }
    }


}
