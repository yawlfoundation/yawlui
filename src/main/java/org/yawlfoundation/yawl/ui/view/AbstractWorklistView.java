package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
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
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
            new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private HorizontalLayout _content;

    private final Participant _user;
    private UserPrivileges _userPrivileges;
    private QueueSet _queueSet;


    public AbstractWorklistView(Participant participant) {
        this(participant, true);
    }


    public AbstractWorklistView(Participant participant, boolean showHeader) {
         super(showHeader);
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
        grid.addColumn(this::getSpecID).setHeader(UiUtil.bold("Specification"));
        grid.addColumn(WorkItemRecord::getDocumentation).setHeader(UiUtil.bold("Documentation"));
        grid.addColumn(this::getEnablementTime).setHeader(UiUtil.bold("Created"));
        grid.addColumn(this::getExpiryTime).setHeader(UiUtil.bold("Expires"));
        grid.addColumn(WorkItemRecord::getResourceStatus).setHeader(UiUtil.bold("Status"));

        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
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

        enableDocumentationEditing(grid);
        return grid;
    }


    private void applyEditorUpdate(Editor<WorkItemRecord> editor, TextField field) {
        if (editor.isOpen()) {
            WorkItemRecord wir = editor.getItem();
            editor.cancel();
            getResourceClient().setWorkItemDocumentation(wir, field.getValue());
        }
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
                Set<Participant> pSet = getResourceClient().getAssignedParticipants(
                        wir.getID(), queue);
                if (pSet == null) {
                    return new Label("Unknown");
                }

                List<Participant> pList = new ArrayList<>(pSet);
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
        if (! checkItemStatus(wir)) return;

        // can't start a work item that is suspended in the engine
        if (wir.hasStatus("Suspended")) {
            Announcement.warn("Unable to start work item: it is currently " +
                    "suspended by the YAWL Engine. Please try again later.");
            return;
        }

        try {
            getResourceClient().startItem(wir.getID(), pid);
            refresh();
            Announcement.success("Item '%s' started", wir.getID());
        }
        catch (IOException | ResourceGatewayException ex) {
            Announcement.error(ex.getMessage());
        }
    }


    // check that the work item still exists in the engine and has matching statuses
    // if not, show a message and refresh the grid
    protected boolean checkItemStatus(WorkItemRecord wir) {
        try {
            WorkItemRecord engineWir = getResourceClient().getItem(wir.getID());
            String viewStatus = wir.getStatus();
            String engineStatus = engineWir.getStatus();
            if (! viewStatus.equals(engineStatus)) {
                handleStaleList(wir.getID(), "is no longer " + viewStatus);
                return false;
            }
            String viewResourceStatus = wir.getResourceStatus();
            String engineResourceStatus = engineWir.getResourceStatus();
            if (! viewResourceStatus.equals(engineResourceStatus)) {
                handleStaleList(wir.getID(), "is no longer " + viewResourceStatus);
                return false;
            }
        }
        catch (ResourceGatewayException | IOException e) {
            handleStaleList(wir.getID(), "is no longer active");
            return false;
        }
        return true;
    }


    private void handleStaleList(String id, String msg) {
        String warning = String.format("Stale worklist: Work item [" + id + "] " + msg +
                        ". The worklist has now been refreshed.");
        Announcement.warn(warning);
        refresh();
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


    private void enableDocumentationEditing(Grid<WorkItemRecord> grid) {
        Binder<WorkItemRecord> binder = new Binder<>(WorkItemRecord.class);
        Editor<WorkItemRecord> editor = grid.getEditor();
        editor.setBinder(binder);

        TextField docoField = new TextField();
        docoField.setWidthFull();

        binder.forField(docoField).bind(WorkItemRecord::getDocumentation,
                WorkItemRecord::setDocumentation);
        grid.getColumns().get(2).setEditorComponent(docoField);  // 3rd col is documentation
        grid.getColumns().get(2).setKey("docoCol");

        // if dbl-click on doco field, open the editor
        grid.addItemDoubleClickListener(e -> {
            if (e.getColumn().getKey() != null) {  // only the doco col key is not null
                editor.editItem(e.getItem());
                Component editorComponent = e.getColumn().getEditorComponent();
                if (editorComponent instanceof Focusable) {
                    ((Focusable) editorComponent).focus();
                }
            }
            else if (editor.isOpen()) {
                editor.cancel();
            }
        });

        // update wir on service side when 'esc' pressed or click outside editor
        docoField.getElement().addEventListener("keydown",
                e -> applyEditorUpdate(editor, docoField))
                .setFilter("event.code === 'Escape'");

        grid.addItemClickListener(e -> applyEditorUpdate(editor, docoField));
    }
    
}
