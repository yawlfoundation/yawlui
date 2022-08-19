package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
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
    

    private boolean _directlyToMe;

    
    public AdminWorklistView(ResourceClient client, Participant participant) {
        super(client, null, participant);
    }


    @Override
    protected QueueSet refreshQueueSet(Participant p) {
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
        if ("Unoffered".equals(wir.getResourceStatus())) {
            ribbon.add(VaadinIcon.HANDSHAKE, "Offer",
                    event -> reassignMultiple(wir, Action.Offer));
            ribbon.add(VaadinIcon.INBOX, "Allocate",
                    event -> reassignSingle(wir, Action.Allocate));
            ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start",
                    event -> reassignSingle(wir, Action.Start));

        }
        else {
            ribbon.add(VaadinIcon.HANDSHAKE,
                    "Reoffer", event -> reassignMultiple(wir, Action.Reoffer));
            ActionIcon reallocate = ribbon.add(VaadinIcon.INBOX,
                    "Reallocate", event -> reassignSingle(wir, Action.Reallocate));
            ActionIcon restart = ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN,
                    "Restart", event -> reassignSingle(wir, Action.Restart));
            switch(wir.getResourceStatus()) {
                case "Offered": reallocate.setEnabled(false);
                case "Allocated": restart.setEnabled(false);     // deliberate no break
            }
        }
        return ribbon;
    }


    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "Refresh", event -> refreshGrid());

        // the only setting currently is 'directly to me'
        if (getParticipant() != null) {
            ribbon.add(VaadinIcon.COG_O, "Settings", e -> settings());
        }
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


     private void reassignSingle(WorkItemRecord wir, Action action) {
        if (_directlyToMe) {
            reassignSingle(wir, getParticpantID(), action);
        }
        else {
            SingleSelectParticipantList listPanel =
                    showSingleSelectParticipantList(wir, action.name());
            if (listPanel != null) {
                listPanel.addOKListener(e -> {
                    getContentPanel().remove(listPanel);
                    String pid = listPanel.getSelectedID();
                    if (pid != null) {
                        reassignSingle(wir, pid, action);
                    }
                });
            }
        }
    }


    private void reassignMultiple(WorkItemRecord wir, Action action) {
        if (_directlyToMe) {
            reassignMultiple(wir, Set.of(getParticpantID()), action);
        }
        else {
            MultiSelectParticipantList listPanel =
                    showMultiSelectParticipantList(wir, action.name());
            if (listPanel != null) {
                listPanel.addOKListener(e -> {
                    getContentPanel().remove(listPanel);
                    Set<String> pids = listPanel.getSelectedIDs();
                    if (!pids.isEmpty()) {
                        reassignMultiple(wir, pids, action);
                    }
                });
            }
        }
    }


    private void reassignSingle(WorkItemRecord wir, String pid, Action action) {
        try {
            switch (action) {
                case Allocate: _resClient.allocateItem(wir.getID(), pid); break;
                case Start: _resClient.startItem(wir.getID(), pid); break;
                case Reallocate: _resClient.reallocateItem(wir.getID(), pid); break;
                case Restart: _resClient.restartItem(wir.getID(), pid); break;
            }
            refreshGrid();
            Announcement.success("%s%s item '%s'", action.name(),
                    (action.name().endsWith("e") ? "d" : "ed"), wir.getID());
        }
        catch (IOException | ResourceGatewayException ex) {
            Announcement.error(ex.getMessage());
        }
    }


    private void reassignMultiple(WorkItemRecord wir, Set<String> pids, Action action) {
        try {
            switch (action) {
                case Offer: _resClient.offerItem(wir.getID(), pids); break;
                case Reoffer: _resClient.reofferItem(wir.getID(), pids); break;
            }
            refreshGrid();
            Announcement.success("%ed item '%s' to %d participant%s",
                    action.name(), wir.getID(), pids.size(), (pids.size() > 1 ? "s" : ""));
        }
        catch (IOException | ResourceGatewayException ex) {
            Announcement.error(ex.getMessage());
        }
     }


    private void settings() {
        int colIndex = getGrid().getColumns().size() - 2;               // 2nd last col

        Checkbox cbx = new Checkbox("Directly to me",
                e -> _directlyToMe = e.getValue());
        cbx.setValue(_directlyToMe);

        Button closeButton = new Button(new Icon("lumo", "cross"),
                e -> setFooterComponent(colIndex, null));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout layout = new HorizontalLayout(cbx, closeButton);
        layout.setAlignItems(Alignment.CENTER);
        layout.setPadding(false);
        layout.setMargin(false);
        setFooterComponent(colIndex, layout);
    }


    private void setFooterComponent(int colIndex, Component component) {
        FooterRow footerRow = getGrid().getFooterRows().get(0);
        footerRow.getCell(getGrid().getColumns().get(colIndex)).setComponent(component);
    }


    private Component getAssignedParticipants(WorkItemRecord wir) {
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

                 if (pList.size() > 1) {
                     ComboBox<Participant> comboBox = new ComboBox<>();
                     comboBox.setItems(pList);
                     comboBox.setItemLabelGenerator(Participant::getFullName);
                     comboBox.setPlaceholder(pList.size() + " participants");
                     comboBox.getElement().getStyle().set("padding", "0");
                     return comboBox;
                 }

                 if (pList.size() == 1) {
                     return new Label(pList.get(0).getFullName());
                 }
             }
             catch (IOException |ResourceGatewayException e) {
                 // fall through to blank label
             }
         }
        return new Label();
    }

}
