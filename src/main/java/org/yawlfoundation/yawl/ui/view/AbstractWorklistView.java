package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Michael Adams
 * @date 5/8/2022
 */
// this css makes a combobox inside a grid transparent (when theme is added to combo)
@CssImport(value = "./styles/combo-in-grid.css", themeFor = "vaadin-input-container")
public abstract class AbstractWorklistView extends AbstractGridView<WorkItemRecord> {

    protected enum Action {
        Offer, Allocate, Start, Reoffer, Reallocate, Restart, Delegate, Complete,
        Suspend, Unsuspend, Pile, Skip, Chain, Deallocate, Accept, Edit
    }

    private static final DateFormat DATE_FORMAT =
                new SimpleDateFormat("MMM dd yyyy H:mm:ss");

    private HorizontalLayout _content;

    private final Participant _user;
    private UserPrivileges _userPrivileges;
    private QueueSet _queueSet;

    
    public AbstractWorklistView(ResourceClient resClient, EngineClient engClient,
                                Participant participant) {
        this(resClient, engClient, participant, true);
    }


    public AbstractWorklistView(ResourceClient resClient, EngineClient engClient,
                                Participant participant, boolean showHeader) {
        super(resClient, engClient, showHeader);
        _user = participant;
        build();
        initCompleted();
    }


     abstract QueueSet refreshQueueSet(Participant p);

    @Override
    List<WorkItemRecord> getItems() {
        _queueSet = refreshQueueSet(_user);
        return getAllWorkItems();
    }

    @Override
    void addColumns(Grid<WorkItemRecord> grid) {
        grid.addColumn(WorkItemRecord::getID).setHeader(UiUtil.bold("Item"));
        grid.addColumn(WorkItemRecord::getRootCaseID).setHeader(UiUtil.bold("Case"));
        grid.addColumn(this::getSpecID).setHeader(UiUtil.bold("Specification"));
        grid.addColumn(this::getEnablementTime).setHeader(UiUtil.bold("Created"));
        grid.addColumn(this::getExpiryTime).setHeader(UiUtil.bold("Expires"));
        grid.addColumn(WorkItemRecord::getResourceStatus).setHeader(UiUtil.bold("Status"));
    }


    @Override
    void configureComponentColumns(Grid<WorkItemRecord> grid) { }

    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createRefreshAction());
    }

    
    @Override
    protected HorizontalLayout createLayout() {
        Component gridPanel = super.createLayout();
        _content = new HorizontalLayout();
        _content.add(gridPanel);
        _content.setFlexGrow(1, gridPanel);
        _content.setSizeFull();
        return _content;
    }

    // to be overridden as required in subclasses
    protected void initCompleted() { }

    protected HorizontalLayout getContentPanel() {
        return _content;
    }


    // this is here because it is used by 2 views
    protected Grid<WorkItemRecord> createAdminGrid() {
        Grid<WorkItemRecord> grid = super.createGrid();
        grid.addComponentColumn(this::getAssignedParticipants).setAutoWidth(true)
                .setFlexGrow(0).setResizable(false).setHeader(UiUtil.bold("Assigned"));

        // reorder columns
        List<Grid.Column<WorkItemRecord>> columns = new ArrayList<>(grid.getColumns());
        Grid.Column<WorkItemRecord> pColumn = columns.remove(columns.size() -1);
        columns.add(columns.size() -1, pColumn);
        grid.setColumnOrder(columns);
        return grid;
    }


    protected void setFooterComponent(int colIndex, Component component) {
        FooterRow footerRow = getGrid().getFooterRows().get(0);
        footerRow.getCell(getGrid().getColumns().get(colIndex)).setComponent(component);
    }


    protected SingleSelectParticipantList showSingleSelectParticipantList(
                WorkItemRecord wir, String action) {
        try {
            return showSingleSelectParticipantList(wir, action, getAllParticipants());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return null;
        }
    }

    
    protected SingleSelectParticipantList showSingleSelectParticipantList(
            WorkItemRecord wir, String action, List<Participant> pList) {
        if (getContentPanel().getChildren().count() > 1) {
            Announcement.warn("An admin action is already in progress.");
            return null;
        }

        String title = String.format("%s Work Item '%s'", action, wir.getID());
        SingleSelectParticipantList listPanel =
                new SingleSelectParticipantList(pList, title);
        listPanel.addCancelListener(e -> getContentPanel().remove(listPanel));
        getContentPanel().add(listPanel);
        return listPanel;
    }


    protected MultiSelectParticipantList showMultiSelectParticipantList(
            WorkItemRecord wir, String action) {
        if (getContentPanel().getChildren().count() > 1) {
            Announcement.warn("An admin action is already in progress.");
            return null;
        }
        try {
            List<Participant> pList = getAllParticipants();
            String title = String.format("%s Work Item '%s'", action, wir.getID());
            MultiSelectParticipantList listPanel =
                    new MultiSelectParticipantList(pList, title);
            listPanel.addCancelListener(e -> getContentPanel().remove(listPanel));
            getContentPanel().add(listPanel);
            return listPanel;
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return null;
        }
    }


    protected Component getAssignedParticipants(WorkItemRecord wir) {
        int queue = -1;
        switch (wir.getResourceStatus()) {
            case "Offered" : queue = WorkQueue.OFFERED; break;
            case "Allocated" : queue = WorkQueue.ALLOCATED; break;
            case "Started" : queue = WorkQueue.STARTED; break;
            case "Suspended" : queue = WorkQueue.SUSPENDED; break;
        }
        if (queue > -1) {
            try {
                List<Participant> pList = new ArrayList<>(
                        getResourceClient().getAssignedParticipants(wir.getID(), queue));

                if (pList.size() > 1) {
                    return buildParticipantCombo(pList);
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


    protected List<Participant> getAllParticipants()
            throws ResourceGatewayException, IOException {
        return getResourceClient().getParticipants();
    }

    protected QueueSet getQueueSet() { return _queueSet; }

    protected Participant getParticipant() { return _user; }

    protected String getParticipantID() {
        return _user != null ? _user.getID() : null;
    }

    protected boolean isAdminUser() { return _user == null; }

    protected boolean hasAdminPrivileges() {
        return isAdminUser() || _user.isAdministrator();
    }

    protected UserPrivileges getUserPrivileges() {
        if (_userPrivileges == null && _user != null) {
            try {
                _userPrivileges = getResourceClient().getUserPrivileges(getParticipantID());
            }
            catch (IOException | ResourceGatewayException e) {
                e.printStackTrace();
            }
        }
        return _userPrivileges;
    }


    protected void startItem(WorkItemRecord wir, String pid) {
        try {
            getResourceClient().startItem(wir.getID(), pid);
            refresh();
            Announcement.success("Item '%s' started", wir.getID());
        }
        catch (IOException | ResourceGatewayException ex) {
            Announcement.error(ex.getMessage());
        }
    }


    private String getSpecID(WorkItemRecord wir) {
        return wir.getSpecURI() + " v" + wir.getSpecVersion();
    }


    private String getEnablementTime(WorkItemRecord wir) {
        return longStringToDateString(wir.getEnablementTimeMs());
    }


    private String getExpiryTime(WorkItemRecord wir) {
        return longStringToDateString(wir.getTimerExpiry());
    }


    private String longStringToDateString(String longStr) {
        long msecs = StringUtil.strToLong(longStr, 0);
        return msecs > 0 ? DATE_FORMAT.format(new Date(msecs)) : "";
    }


    private List<WorkItemRecord> getAllWorkItems() {
        List<WorkItemRecord> items = new ArrayList<>();
        for (int i = WorkQueue.OFFERED; i <= WorkQueue.WORKLISTED; i++) {
            items.addAll(_queueSet.getQueuedWorkItems(i));
        }
        return items;
    }


    private int getItemCount() {
        int count = 0;
        for (int i = WorkQueue.OFFERED; i <= WorkQueue.WORKLISTED; i++) {
            count += _queueSet.getQueueSize(i);
        }
        return count;
    }
}
