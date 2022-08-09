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
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.dialog.AdminWorklistOptionsDialog;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class AdminWorklistView extends AbstractWorklistView {

    private boolean _directlyToMe = false;
    private boolean _participantListIsVisible = false;

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
                ribbon.add(VaadinIcon.HANDSHAKE, "#A37063", "Offer",
                        event -> offerItem(wir));
                ribbon.add(VaadinIcon.INBOX, "#0066FF", "Allocate",
                        event -> allocateItem(wir));
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Start",
                        event -> startItem(wir));
                break;
            }

            // todo standardise colours with ActionRibbon constants
            case "Started" :
            case "Suspended" : {
                ribbon.add(VaadinIcon.HANDSHAKE, "#A37063", "Reoffer",
                        event -> reofferItem(wir));
                ribbon.add(VaadinIcon.INBOX, "#0066FF", "Reallocate",
                        event -> reallocateItem(wir));
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Restart",
                        event -> restartItem(wir));
                break;
            }
            case "Allocated" : {
                ribbon.add(VaadinIcon.HANDSHAKE, "#A37063", "Reoffer",
                        event -> reofferItem(wir));
                ribbon.add(VaadinIcon.INBOX, "#0066FF", "Reallocate",
                        event -> reallocateItem(wir));
                ribbon.add(VaadinIcon.CARET_RIGHT);
                break;
            }
            case "Offered" : {
                ribbon.add(VaadinIcon.HANDSHAKE, "#A37063", "Reoffer",
                        event -> reofferItem(wir));
                ribbon.add(VaadinIcon.INBOX);
                ribbon.add(VaadinIcon.CARET_RIGHT);
                break;
            }
        }

        return ribbon;
    }


    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "#0066FF", "Refresh", event ->
                refreshGrid());

        ribbon.add(VaadinIcon.COG_O, "#0066FF", "Settings",
                event -> {
                    AdminWorklistOptionsDialog dialog = new AdminWorklistOptionsDialog();
                    dialog.getOK().addClickListener(e -> {
                        _directlyToMe = dialog.isChecked();
                        dialog.close();
                    });
                    dialog.open();
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


    private void offerItem(WorkItemRecord wir) {
        MultiSelectParticipantList listPanel =
                showMultiSelectParticipantList(wir, "Offer");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);
                try {
                    Set<String> ids = listPanel.getSelectedIDs();
                    _resClient.offerItem(wir.getID(), ids);
                    refreshGrid();
                    Announcement.success("Offered item '%s' to %d participants",
                            wir.getID(), ids.size());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void allocateItem(WorkItemRecord wir) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Allocate");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    _resClient.allocateItem(wir.getID(), listPanel.getSelectedID());
                    refreshGrid();
                    Announcement.success("Allocated item '%s'", wir.getID());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void startItem(WorkItemRecord wir) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Start");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    _resClient.startItem(wir.getID(), listPanel.getSelectedID());
                    refreshGrid();
                    Announcement.success("Started item '%s'", wir.getID());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void reofferItem(WorkItemRecord wir) {
        MultiSelectParticipantList listPanel =
                showMultiSelectParticipantList(wir, "Reoffer");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);
                try {
                    Set<String> ids = listPanel.getSelectedIDs();
                    _resClient.reofferItem(wir.getID(), ids);
                    refreshGrid();
                    Announcement.success("Reoffered item '%s' to %d participants",
                            wir.getID(), ids.size());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void reallocateItem(WorkItemRecord wir) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Reallocate");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    _resClient.reallocateItem(wir.getID(), listPanel.getSelectedID());
                    refreshGrid();
                    Announcement.success("Reallocated item '%s'", wir.getID());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void restartItem(WorkItemRecord wir) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Restart");
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    _resClient.restartItem(wir.getID(), listPanel.getSelectedID());
                    refreshGrid();
                    Announcement.success("Restarted item '%s'", wir.getID());
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }



    // todo filtering of parts
    private SingleSelectParticipantList showSingleSelectParticipantList(
            WorkItemRecord wir, String action) {
        if (getContentPanel().getChildren().count() > 1) {
            Announcement.error("An admin action is already in progress.");
            return null;
        }
        try {
            List<Participant> pList = getAllParticipants();
            SingleSelectParticipantList listPanel =
                    new SingleSelectParticipantList(pList, action, wir.getID());
            listPanel.addCancelListener(e -> getContentPanel().remove(listPanel));
            getContentPanel().add(listPanel);
            return listPanel;
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return null;
        }
    }

    
    private MultiSelectParticipantList showMultiSelectParticipantList(
            WorkItemRecord wir, String action) {
        if (getContentPanel().getChildren().count() > 1) {
            Announcement.error("An admin action is already in progress.");
            return null;
        }
        try {
            List<Participant> pList = getAllParticipants();
            MultiSelectParticipantList listPanel =
                    new MultiSelectParticipantList(pList, action, wir.getID());
            listPanel.addCancelListener(e -> getContentPanel().remove(listPanel));
            getContentPanel().add(listPanel);
            return listPanel;
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return null;
        }
    }


    private List<Participant> getAllParticipants() throws ResourceGatewayException, IOException {
        return _resClient.getParticipants();
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
