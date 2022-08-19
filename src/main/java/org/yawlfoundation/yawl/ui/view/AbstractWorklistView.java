package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
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
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
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
public abstract class AbstractWorklistView extends AbstractView {

    protected enum Action {
        Offer, Allocate, Start, Reoffer, Reallocate, Restart, Delegate
    }

    private static final DateFormat DATE_FORMAT =
                new SimpleDateFormat("MMM dd yyyy H:mm:ss");

    private final Grid<WorkItemRecord> _grid;
    private final HorizontalLayout _content;
    private H4 _header;

    private final Participant _user;
    private UserPrivileges _userPrivileges;
    private QueueSet _queueSet;

    public AbstractWorklistView(ResourceClient resClient, EngineClient engClient, Participant p) {
        super(resClient, engClient);
        _user = p;
        _queueSet = refreshQueueSet(p);
        _grid = createGrid();
        _content = createPanel();
        add(_content);
        setSizeFull();
    }

    protected abstract QueueSet refreshQueueSet(Participant p);

    protected abstract String getTitle();

    protected abstract ActionRibbon createColumnActions(WorkItemRecord wir);

    protected abstract ActionRibbon createFooterActions();


    protected void refreshGrid() {
        _queueSet = refreshQueueSet(_user);
        _grid.setItems(getAllWorkItems());
        _grid.getDataProvider().refreshAll();
        _grid.recalculateColumnWidths();
        refreshHeader(_header, getTitle(), getItemCount());
    }


    protected HorizontalLayout getContentPanel() {
        return _content;
    }


    protected Grid<WorkItemRecord> getGrid() { return _grid; }


    protected HorizontalLayout createPanel() {
        _header = new H4(String.format("%s (%d)", getTitle(), getItemCount()));
        UnpaddedVerticalLayout gridPanel = createGridPanel(_header, _grid);
        HorizontalLayout content = new HorizontalLayout();
        content.add(gridPanel);
        content.setFlexGrow(1, gridPanel);
        content.setSizeFull();
        return content;
    }


    protected Grid<WorkItemRecord> createGrid() {
        Grid<WorkItemRecord> grid = new Grid<>();
        grid.setItems(getAllWorkItems());
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(WorkItemRecord::getID).setHeader(UiUtil.bold("Item"));
        grid.addColumn(WorkItemRecord::getRootCaseID).setHeader(UiUtil.bold("Case"));
        grid.addColumn(this::getSpecID).setHeader(UiUtil.bold("Specification"));
        grid.addColumn(this::getEnablementTime).setHeader(UiUtil.bold("Created"));
        grid.addColumn(this::getExpiryTime).setHeader(UiUtil.bold("Expires"));
        grid.addColumn(WorkItemRecord::getResourceStatus).setHeader(UiUtil.bold("Status"));
        Grid.Column<WorkItemRecord> actionColumn = grid.addComponentColumn(
                this::createColumnActions);
        configureGrid(grid);
        configureActionColumn(actionColumn);
        addGridFooter(grid, createFooterActions());
        return grid;
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

        SingleSelectParticipantList listPanel =
                new SingleSelectParticipantList(pList, action, wir.getID());
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


    protected List<Participant> getAllParticipants() throws ResourceGatewayException, IOException {
        return _resClient.getParticipants();
    }

    protected QueueSet getQueueSet() { return _queueSet; }

    protected ResourceClient getClient() { return _resClient; }

    protected Participant getParticipant() { return _user; }

    protected String getParticpantID() {
        return _user != null ? _user.getID() : null;
    }

    protected boolean isAdminUser() { return _user == null; }

    protected boolean hasAdminPrivileges() {
        return isAdminUser() || _user.isAdministrator();
    }

    protected UserPrivileges getUserPrivileges() {
        if (_userPrivileges == null && _user != null) {
            try {
                _userPrivileges = _resClient.getUserPrivileges(getParticpantID());
            }
            catch (IOException | ResourceGatewayException e) {
                e.printStackTrace();
            }
        }
        return _userPrivileges;
    }


    protected void startItem(WorkItemRecord wir, String pid) {
        try {
            _resClient.startItem(wir.getID(), pid);
            refreshGrid();
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
