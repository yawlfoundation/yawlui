package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.AddedIcons;
import org.yawlfoundation.yawl.ui.util.Settings;

import java.io.IOException;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class AdminWorklistView extends AbstractWorklistView {

    private boolean _settingsVisible = false;
    
    public AdminWorklistView(ResourceClient client, Participant participant) {
        super(client, null, participant);
    }


    @Override
    protected QueueSet refreshQueueSet(Participant p) {
        try {
            return getResourceClient().getAdminWorkQueues();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return new QueueSet(null, QueueSet.setType.adminSet, false);
        }
    }


    @Override
    protected String getTitle() {
        return "Admin Worklist";
    }


    @Override
    protected void addItemActions(WorkItemRecord wir, ActionRibbon ribbon) {
        if ("Unoffered".equals(wir.getResourceStatus())) {
            ActionIcon offer = new ActionIcon(AddedIcons.HUB, null,
                    "Offer", event -> reassignMultiple(wir, Action.Offer));
            ribbon.add(offer);
            ActionIcon allocate = new ActionIcon(AddedIcons.DOWNLOAD, null,
                    "Allocate", event -> reassignSingle(wir, Action.Allocate));
            ribbon.add(allocate);
            ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start",
                    event -> reassignSingle(wir, Action.Start));
        }
        else {
            ActionIcon reoffer = new ActionIcon(AddedIcons.HUB, null,
                    "Reoffer", event -> reassignMultiple(wir, Action.Reoffer));
            ribbon.add(reoffer);
            ActionIcon reallocate = new ActionIcon(AddedIcons.DOWNLOAD, null,
                    "Reallocate", event -> reassignSingle(wir, Action.Reallocate));
            ribbon.add(reallocate);
            ActionIcon restart = ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN,
                    "Restart", event -> reassignSingle(wir, Action.Restart));
            switch(wir.getResourceStatus()) {
                case "Offered": reallocate.setEnabled(false);
                case "Allocated": restart.setEnabled(false);     // deliberate no break
            }
        }
    }

    @Override
    void addFooterActions(ActionRibbon ribbon) {

        // the only setting currently is 'directly to me'
        if (getParticipant() != null) {
            ribbon.add(VaadinIcon.COG_O, "Settings", e -> settings());
        }

        super.addFooterActions(ribbon);
    }


    @Override
    protected Grid<WorkItemRecord> createGrid() {
        return createAdminGrid();
    }


     private void reassignSingle(WorkItemRecord wir, Action action) {
        if (Settings.isDirectlyToMe()) {
            reassignSingle(wir, getParticipantID(), action);
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
        if (Settings.isDirectlyToMe()) {
            reassignMultiple(wir, Set.of(getParticipantID()), action);
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
                case Allocate: getResourceClient().allocateItem(wir.getID(), pid); break;
                case Start: getResourceClient().startItem(wir.getID(), pid); break;
                case Reallocate: getResourceClient().reallocateItem(wir.getID(), pid); break;
                case Restart: getResourceClient().restartItem(wir.getID(), pid); break;
            }
            refresh();
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
                case Offer: getResourceClient().offerItem(wir.getID(), pids); break;
                case Reoffer: getResourceClient().reofferItem(wir.getID(), pids); break;
            }
            refresh();
            Announcement.success("%sed item '%s' to %d participant%s",
                    action.name(), wir.getID(), pids.size(), (pids.size() > 1 ? "s" : ""));
        }
        catch (IOException | ResourceGatewayException ex) {
            Announcement.error(ex.getMessage());
        }
    }


    private void settings() {
        int colIndex = getGrid().getColumns().size() - 2;               // 2nd last col

        Checkbox cbx = null;
        if (! _settingsVisible) {
            cbx = new Checkbox("Directly to me",
                    e -> Settings.setDirectlyToMe(e.getValue()));
            cbx.setValue(Settings.isDirectlyToMe());
            getGrid().getColumns().get(colIndex).setWidth("145px");
        }

        setFooterComponent(colIndex, cbx);
        _settingsVisible = ! _settingsVisible;                          // toggle
    }

}
