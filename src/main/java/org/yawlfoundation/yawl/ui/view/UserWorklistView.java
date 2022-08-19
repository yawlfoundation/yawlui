package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.dialog.SingleValueDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class UserWorklistView extends AbstractWorklistView {


    public UserWorklistView(ResourceClient resClient, EngineClient engClient, Participant p) {
        super(resClient, engClient, p);
    }


    @Override
    protected QueueSet refreshQueueSet(Participant p) {
        try {
            return getClient().getUserWorkQueues(p.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return new QueueSet(null, QueueSet.setType.adminSet, false);
        }
    }


    @Override
    protected String getTitle() {
        return "My Work List";
    }


    //todo finish task privs
    @Override
    protected ActionRibbon createColumnActions(WorkItemRecord wir) {
        TaskPrivileges taskPrivileges = getTaskPrivileges(wir);
        switch(wir.getResourceStatus()) {
            case "Offered" : return createOfferedRibbon(wir);
            case "Allocated" : return createAllocatedRibbon(wir, taskPrivileges);
            case "Started" : return createStartedRibbon(wir, taskPrivileges);
            case "Suspended" : return createSuspendedRibbon(wir);
            default : return null;
        }
    }


    private ActionRibbon createOfferedRibbon(WorkItemRecord wir) {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.INBOX,"Accept", e -> accept(wir));
        ContextMenu menu = addContextMenu(ribbon);
        menu.addItem("Accept & Start", e -> acceptAndStart(wir));
        if (userMayChain(wir)) {
            menu.addItem("Chain", e -> chain(wir));
        }
        return ribbon;
    }


    private ActionRibbon createAllocatedRibbon(WorkItemRecord wir, TaskPrivileges privileges) {
        ActionRibbon ribbon = new ActionRibbon();
        if (userMayStart(wir)) {
            ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start",
                    event -> startItem(wir, getParticpantID()));
        }
        else {
            ribbon.add(VaadinIcon.CARET_RIGHT);
        }

        ContextMenu menu = addContextMenu(ribbon);

        if (userMaySkip(wir, privileges)) {
            menu.addItem("Skip", e -> skip(wir));
        }
        if (userMayDeallocate(privileges)) {
            menu.addItem("Deallocate", e -> deallocate(wir));
        }
        if (userMayDelegate(privileges)) {
            menu.addItem("Delegate", e -> delegate(wir));
        }
        if (userMayPile(wir, privileges)) {
            menu.addItem("Pile", e -> pile(wir));
        }
        if (menu.getItems().isEmpty()) {
            menu.setEnabled(false);
        }
        return ribbon;
    }


    private ActionRibbon createStartedRibbon(WorkItemRecord wir, TaskPrivileges privileges) {
        ActionRibbon ribbon = new ActionRibbon();
        if (userMayEdit(wir)) {
            ribbon.add(VaadinIcon.PENCIL, "View/Edit", event -> edit(wir));
        }
        else {
            ribbon.add(VaadinIcon.PENCIL);   // show disabled
        }
        ContextMenu menu = addContextMenu(ribbon);
        if (userMaySuspend(privileges)) {
            menu.addItem("Suspend", e -> suspend(wir));
        }
        if (userMayComplete(wir)) {
            menu.addItem("Complete", e -> complete(wir));
        }
        if (userMayReallocateStateful(privileges)) {
            menu.addItem("Reallocate (stateful)", e -> reallocate(wir, true));
        }
        if (userMayReallocateStateless(privileges)) {
            menu.addItem("Reallocate (stateless)", e -> reallocate(wir, false));
        }
        if (userMayAddNewInstance(wir)) {
            menu.addItem("Add Instance", e -> newInstance(wir));
        }
        if (menu.getItems().isEmpty()) {
             menu.setEnabled(false);
        }
        return ribbon;
    }


    private ActionRibbon createSuspendedRibbon(WorkItemRecord wir) {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.TIME_BACKWARD, "Unsuspend", event -> unsuspend(wir));
        ribbon.add(VaadinIcon.MENU);
        return ribbon;
    }


    private List<Participant> getSubordinatesOf() {
        try {
            return new ArrayList<>(_resClient.getSubordinateParticpants(getParticpantID()));
        }
        catch (IOException |ResourceGatewayException e) {
            Announcement.warn("Unable to gather subordinates list");
        }
        return Collections.emptyList();
    }


    private void newInstance(WorkItemRecord wir) {
        try {
            if (_engClient.canCreateNewInstance(wir.getID())) {
                TaskInformation taskInfo = _engClient.getTaskInformation(wir);
                YParameter formalParam = taskInfo.getParamSchema().getFormalInputParam();

                SingleValueDialog dialog = new SingleValueDialog("Add New Instance",
                        "Please enter a valid data value for the named parameter");
                dialog.setPrompt(formalParam.getName());
                dialog.getOKButton().addClickListener(c -> {
                    String value = dialog.getValue();
                    String data = StringUtil.wrap(value, formalParam.getName());
                    try {
                        WorkItemRecord newWir = _engClient.createNewInstance(wir.getID(), data);
                        _resClient.allocateItem(newWir.getID(), getParticpantID());
                        refreshGrid();
                    }
                    catch (IOException | ResourceGatewayException e) {
                        throw new RuntimeException(e);
                    }
                });
                dialog.open();
            }
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void reallocate(WorkItemRecord wir, boolean stateful) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Reallocate",
                        getSubordinatesOf());
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    Participant pTo = listPanel.getSelected();
                    if (pTo != null) {
                        _resClient.reallocateItem(wir.getID(), getParticpantID(),
                                pTo.getID(), stateful);
                        refreshGrid();
                        Announcement.success("Reallocated %s item '%s' to %s",
                                (stateful ? "stateful" : "stateless"),
                                wir.getID(), pTo.getFullName());
                    }
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void complete(WorkItemRecord wir) {
        try {
            _resClient.completeItem(wir.getID(), getParticpantID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void suspend(WorkItemRecord wir) {
        try {
            _resClient.suspendItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' suspended", wir.getID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void pile(WorkItemRecord wir) {
        try {
            _resClient.pileItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' suspended", wir.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void delegate(WorkItemRecord wir) {
        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Delegate", getSubordinatesOf());
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    Participant pTo = listPanel.getSelected();
                    if (pTo != null) {
                        _resClient.delegateItem(wir.getID(), getParticpantID(),
                                pTo.getID());
                        refreshGrid();
                        Announcement.success("Delegated item '%s' to %s",
                                wir.getID(), pTo.getFullName());
                    }
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error(ex.getMessage());
                }
            });
        }
    }


    private void deallocate(WorkItemRecord wir) {
        try {
            _resClient.deallocateItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' deallocated", wir.getID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void skip(WorkItemRecord wir) {
        try {
            _resClient.skipItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' skipped", wir.getID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void chain(WorkItemRecord wir) {
        try {
            _resClient.chainCase(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Case %s chained", wir.getRootCaseID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }

    }

    private void acceptAndStart(WorkItemRecord wir) {
        accept(wir);

        // only start it next if the item is not already set to system start
        if (getQueueSet().hasWorkItemInQueue(wir.getID(), WorkQueue.ALLOCATED)) {
            startItem(wir, getParticpantID());
        }
    }


    private void unsuspend(WorkItemRecord wir) {
        try {
            _resClient.unsuspendItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' unsuspended", wir.getID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void edit(WorkItemRecord wir) {

    }


    private void accept(WorkItemRecord wir) {
        try {
            _resClient.acceptItem(wir.getID(), getParticpantID());
            refreshGrid();
            Announcement.success("Item '%s' accepted", wir.getID());
        }
        catch (ResourceGatewayException | IOException e) {
            Announcement.error(e.getMessage());
        }
    }



    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "Refresh", event -> refreshGrid());
        return ribbon;
    }


    private ContextMenu addContextMenu(ActionRibbon ribbon) {
        ActionIcon icon = ribbon.add(VaadinIcon.MENU,
                "Other Actions", null);
        ContextMenu menu = new ContextMenu(icon);
        menu.setOpenOnClick(true);
        return menu;
    }


    private TaskPrivileges getTaskPrivileges(WorkItemRecord wir) {
        try {
            return _resClient.getTaskPrivileges(wir.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            e.printStackTrace();
            return null;
        }
    }


    // this is engine-suspended, not resource-suspended  
    private boolean isNotSuspended(WorkItemRecord wir) {
        return !wir.hasStatus(WorkItemRecord.statusSuspended);
    }

    
    private boolean userMayStart(WorkItemRecord wir) {
        if (hasAdminPrivileges()) return true;

        // continue - not "admin" user
        if (! isNotSuspended(wir)) return false;

        QueueSet qSet = getQueueSet();
        int startedItemCount = qSet.getQueueSize(WorkQueue.STARTED) +
                qSet.getQueueSize(WorkQueue.SUSPENDED);

        // ok to start if:
        //  1. user has no started items or can have multiple started items
        //     AND
        //  2. can start items in any order OR is first item in queue
        return (startedItemCount == 0 || getUserPrivileges().canStartConcurrent()) &&
                (getUserPrivileges().canChooseItemToStart() ||
                        getUserPrivileges().canReorder() ||
                isOldestQueuedItem(wir, qSet.getQueuedWorkItems(WorkQueue.ALLOCATED)));
    }


    private boolean userMayChain(WorkItemRecord wir) {
        return hasAdminPrivileges() ||
                (isNotSuspended(wir) && getUserPrivileges().canChainExecution());
    }


    private boolean userMaySkip(WorkItemRecord wir, TaskPrivileges privileges) {
        return hasAdminPrivileges() ||
                (isNotSuspended(wir) && privileges.canSkip(getParticipant()));
    }


    private boolean userMayPile(WorkItemRecord wir, TaskPrivileges privileges) {
        return hasAdminPrivileges() ||
                (isNotSuspended(wir) && privileges.canPile(getParticipant()));
    }


    private boolean userMayDeallocate(TaskPrivileges privileges) {
        return hasAdminPrivileges() || privileges.canDeallocate(getParticipant());
    }


    private boolean userMayDelegate(TaskPrivileges privileges) {
        return hasAdminPrivileges() ||
                (privileges.canDelegate(getParticipant()) &&
                        ! getSubordinatesOf().isEmpty());
    }


    private boolean userMaySuspend(TaskPrivileges privileges) {
        return hasAdminPrivileges() || privileges.canSuspend(getParticipant());
    }


    private boolean userMayReallocateStateful(TaskPrivileges privileges) {
        return hasAdminPrivileges() ||
                (privileges.canReallocateStateful(getParticipant()) &&
                ! getSubordinatesOf().isEmpty());
    }


    private boolean userMayReallocateStateless(TaskPrivileges privileges) {
        return hasAdminPrivileges() ||
                (privileges.canReallocateStateless(getParticipant()) &&
                        ! getSubordinatesOf().isEmpty());
    }


    // todo - test this works for empty decomp
    private boolean userMayEdit(WorkItemRecord wir) {
        return hasAdminPrivileges() ||
                ! (wir.getDataList() == null && wir.getCustomFormURL() == null);
    }


    // todo - when 'save' button used on dyn form, validate data (to ensure any saved
    //  data is always valid)
    private boolean userMayComplete(WorkItemRecord wir) {
        return hasAdminPrivileges() || wir.getDataList() == null;
    }


    private boolean userMayAddNewInstance(WorkItemRecord wir) {
        try {
            return wir.isDynamicCreationAllowed() &&
                    _engClient.canCreateNewInstance(wir.getID());
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean isOldestQueuedItem(WorkItemRecord wir,
                                       Set<WorkItemRecord> queuedItems) {
        long wirCreated = StringUtil.strToLong(wir.getEnablementTimeMs(), 0);
        for (WorkItemRecord item : queuedItems) {
            long itemCreated = StringUtil.strToLong(item.getEnablementTimeMs(), 0);
            if (itemCreated < wirCreated) {
                return false;
            }
        }
        return true;
    }

}
