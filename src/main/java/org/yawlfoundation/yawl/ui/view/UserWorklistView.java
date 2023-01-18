package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.jdom2.Element;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.dialog.SingleValueDialog;
import org.yawlfoundation.yawl.ui.dynform.CustomFormLauncher;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ChainedCase;
import org.yawlfoundation.yawl.ui.service.PiledTask;
import org.yawlfoundation.yawl.ui.util.InstalledServices;
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

    private final CustomFormLauncher _customFormLauncher;

    private ActionIcon _chainedIcon;
    private ActionIcon _piledIcon;
    private ContextMenu _chainedList;
    private ContextMenu _piledList;


    public UserWorklistView(Participant p, String customFormHandle) {
        super(p);
        _customFormLauncher = new CustomFormLauncher(customFormHandle);
    }


    @Override
    protected QueueSet refreshQueueSet(Participant p) {
        try {
            return getResourceClient().getUserWorkQueues(p.getID());
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


    @Override
    void addItemActions(WorkItemRecord item, ActionRibbon ribbon) {
        TaskPrivileges taskPrivileges = getTaskPrivileges(item);
        boolean hasWorklets = new InstalledServices().hasWorkletService();
        switch(item.getResourceStatus()) {
            case "Offered" : createOfferedRibbon(item, ribbon, hasWorklets); break;
            case "Allocated" : createAllocatedRibbon(item, ribbon, taskPrivileges, hasWorklets); break;
            case "Started" : createStartedRibbon(item, ribbon, taskPrivileges, hasWorklets); break;
            case "Suspended" : createSuspendedRibbon(item, ribbon); break;
        }
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        UserPrivileges up = getParticipant().getUserPrivileges();
        if (hasAdminPrivileges() || up.canChainExecution()) {
            createChainedAction(ribbon);
        }
        createPiledAction(ribbon);
        super.addFooterActions(ribbon);
    }


    @Override
    protected void refresh() {
        super.refresh();
        if (_chainedList != null) {
            refreshChainedList();
        }
        refreshPiledList();
    }


    private void createPiledAction(ActionRibbon ribbon) {
        _piledIcon = ribbon.add(VaadinIcon.STOCK, "Piled Tasks", null);
        _piledList = createContextMenu(_piledIcon);
        refreshPiledList();
    }


    private void refreshPiledList() {
        _piledList.removeAll();
        try {
            Set<PiledTask> piledTasks = getResourceClient().getPiledTasks(getParticipantID());
            if (! piledTasks.isEmpty()) {
                for (PiledTask piledTask : piledTasks) {
                    ActionIcon removeIcon = new ActionIcon(VaadinIcon.CLOSE_SMALL,
                            ActionIcon.RED, "Unpile", event -> {
                        try {
                            getResourceClient().unpileTask(piledTask, getParticipantID());
                            Announcement.success("Unpiled Task: " + piledTask);
                            refresh();
                        }
                        catch (IOException | ResourceGatewayException ioe) {
                            Announcement.error("Failed to unpile task '%s': %s",
                                    piledTask, ioe.getMessage());
                        }
                    });
                    removeIcon.getStyle().set("margin-left", "auto");
                    HorizontalLayout layout = new HorizontalLayout(
                            new Span(piledTask.toString()), removeIcon);
                    layout.setPadding(false);
                    _piledList.addItem(layout);
                }
            }
            _piledIcon.setEnabled(! piledTasks.isEmpty());
        }
        catch (ResourceGatewayException | IOException e) {
            _piledIcon.setEnabled(false);
        }
    }

    
    private void createChainedAction(ActionRibbon ribbon) {
        _chainedIcon = ribbon.add(VaadinIcon.LINK, "Chained Cases", null);
        _chainedList = createContextMenu(_chainedIcon);
        refreshChainedList();
    }


    private void refreshChainedList() {
        _chainedList.removeAll();
        try {
            Set<ChainedCase> chainedCases = getResourceClient()
                    .getChainedCases(getParticipantID());
            if (! chainedCases.isEmpty()) {
                for (ChainedCase chainedCase : chainedCases) {
                    ActionIcon removeIcon = new ActionIcon(VaadinIcon.CLOSE_SMALL,
                            ActionIcon.RED, "Unchain", event -> {
                        try {
                            getResourceClient().unchainCase(chainedCase.getCaseID());
                            Announcement.success("Unchained Case: " + chainedCase);
                            refresh();
                        }
                        catch (IOException | ResourceGatewayException ioe) {
                            Announcement.error("Failed to unchained Case %s: %s",
                                    chainedCase, ioe.getMessage());
                        }
                    });
                    removeIcon.getStyle().set("margin-left", "auto");
                    HorizontalLayout layout = new HorizontalLayout(
                            new Span(chainedCase.toString()), removeIcon);
                    layout.setPadding(false);
                    _chainedList.addItem(layout);
                }
            }
            _chainedIcon.setEnabled(! chainedCases.isEmpty());
        }
        catch (ResourceGatewayException | IOException e) {
            _chainedIcon.setEnabled(false);
        }
    }


    private void createOfferedRibbon(WorkItemRecord wir, ActionRibbon ribbon, boolean hasWorklets) {
        ribbon.add(VaadinIcon.CHECK,"Accept",
                e -> performAction(Action.Accept, wir));
        ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN,"Accept & Start",
                e -> acceptAndStart(wir));

        ContextMenu menu = addContextMenu(ribbon);
        if (userMayChain(wir)) {
            menu.addItem("Chain", e -> performAction(Action.Chain, wir));
        }
        createRaiseExceptionAction(ribbon, menu, wir, hasWorklets);
    }


    private void createAllocatedRibbon(WorkItemRecord wir, ActionRibbon ribbon,
                                       TaskPrivileges privileges, boolean hasWorklets) {
        if (userMayStart(wir)) {
            ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start",
                    event -> startItem(wir, getParticipantID()));
        }
        else {
            ribbon.add(VaadinIcon.CARET_RIGHT);
        }
        if (userMaySkip(wir, privileges)) {
            ribbon.add(VaadinIcon.ARROW_FORWARD, "Skip",
                    e -> performAction(Action.Skip, wir));
        }
        else {
            ribbon.add(VaadinIcon.ARROW_FORWARD);
        }

        ContextMenu menu = addContextMenu(ribbon);
        if (userMayDeallocate(privileges)) {
            menu.addItem("Deallocate", e -> performAction(Action.Deallocate, wir));
        }
        if (userMayDelegate(privileges)) {
            menu.addItem("Delegate", e -> delegate(wir));
        }
        if (userMayPile(wir, privileges)) {
            menu.addItem("Pile", e -> performAction(Action.Pile, wir));
        }
        createRaiseExceptionAction(ribbon, menu, wir, hasWorklets);
    }


    private void createStartedRibbon(WorkItemRecord wir, ActionRibbon ribbon,
                                     TaskPrivileges privileges, boolean hasWorklets) {
        if (userMayEdit(wir)) {
            ribbon.add(createEditAction(event -> {
                edit(wir);
                ribbon.reset();
            }));
        }
        else {
            ribbon.add(VaadinIcon.PENCIL);   // show disabled
        }
        if (userMayComplete(wir)) {
            ribbon.add(VaadinIcon.OUTBOX, "Complete",
                    event -> performAction(Action.Complete, wir));
        }
        else {
            ribbon.add(VaadinIcon.OUTBOX);   // show disabled
        }

        ContextMenu menu = addContextMenu(ribbon);
        if (userMaySuspend(privileges)) {
            menu.addItem("Suspend", e -> performAction(Action.Suspend, wir));
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
        createRaiseExceptionAction(ribbon, menu, wir, hasWorklets);
    }


    private void createSuspendedRibbon(WorkItemRecord wir, ActionRibbon ribbon) {
        ActionIcon icon = new ActionIcon(VaadinIcon.TIME_BACKWARD,
                ActionIcon.DEFAULT_HOVER, "Unsuspend",
                event -> performAction(Action.Unsuspend, wir));
        icon.getStyle().set("margin-right", "32px");           // empty space to right
        ribbon.add(icon);
        ribbon.add(VaadinIcon.MENU);
    }


    private void createRaiseExceptionAction(ActionRibbon ribbon, ContextMenu menu,
                                            WorkItemRecord wir, boolean hasWorklets) {
        if (hasWorklets) {
            menu.addItem("Raise Exception", event -> {
                raiseExternalException(wir);
                ribbon.reset();
            });
        }
    }


    private List<Participant> getSubordinatesOf() {
        try {
            return new ArrayList<>(getResourceClient().getSubordinateParticpants(
                    getParticipantID()));
        }
        catch (IOException |ResourceGatewayException e) {
            Announcement.warn("Unable to gather subordinates list");
        }
        return Collections.emptyList();
    }


    private void newInstance(WorkItemRecord wir) {
        try {
            if (getEngineClient().canCreateNewInstance(wir.getID())) {
                TaskInformation taskInfo = getEngineClient().getTaskInformation(wir);
                YParameter formalParam = taskInfo.getParamSchema().getFormalInputParam();

                SingleValueDialog dialog = new SingleValueDialog("Add New Instance",
                        "Please enter a valid data value for the named parameter");
                dialog.setPrompt(formalParam.getName());
                dialog.getOKButton().addClickListener(c -> {
                    String value = dialog.getValue();
                    String data = StringUtil.wrap(value, formalParam.getName());
                    try {
                        WorkItemRecord newWir = getEngineClient().createNewInstance(
                                wir.getID(), data);
                        getResourceClient().allocateItem(newWir.getID(), getParticipantID());
                        refresh();
                    }
                    catch (IOException | ResourceGatewayException e) {
                        throw new RuntimeException(e);
                    }
                });
                dialog.open();
            }
        }
        catch (IOException e) {
            Announcement.error("Failed to create new instance: " + e.getMessage());
        }
    }


    private void reallocate(WorkItemRecord wir, boolean stateful) {
        if (! checkItemStatus(wir)) return;

        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Reallocate",
                        getSubordinatesOf());
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    Participant pTo = listPanel.getSelected();
                    if (pTo != null) {
                        getResourceClient().reallocateItem(wir.getID(), getParticipantID(),
                                pTo.getID(), stateful);
                        refresh();
                        Announcement.success("Reallocated %s item '%s' to %s",
                                (stateful ? "stateful" : "stateless"),
                                wir.getID(), pTo.getFullName());
                    }
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error("Failed to reallocate item: " + ex.getMessage());
                }
            });
        }
    }


    private synchronized void performAction(Action action, WorkItemRecord wir) {
        if (! checkItemStatus(wir)) return;

        try {
            String successMsg = String.format("%s%s item '%s'", action.name(),
                    (action.name().endsWith("e") ? "d" : "ed"), wir.getID());
            String pid = getParticipantID();
            switch (action) {
                case Accept : getResourceClient().acceptItem(wir.getID(), pid); break;
                case Complete : getResourceClient().completeItem(wir, pid); break;
                case Suspend : getResourceClient().suspendItem(wir.getID(), pid); break;
                case Unsuspend : getResourceClient().unsuspendItem(wir.getID(), pid); break;
                case Deallocate : getResourceClient().deallocateItem(wir.getID(), pid); break;
                case Skip : getResourceClient().skipItem(wir.getID(), pid); break;
                case Pile : getResourceClient().pileItem(wir.getID(), pid); break;
                case Chain : {
                    getResourceClient().chainCase(wir.getID(), pid);
                    successMsg = String.format("Case %s chained", wir.getRootCaseID());
                    break;
                }
            }
            refresh();
            Announcement.success(successMsg);
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to %s item: %s:",
                    action.name().toLowerCase(), e.getMessage());
        }
    }


    private void delegate(WorkItemRecord wir) {
        if (! checkItemStatus(wir)) return;

        SingleSelectParticipantList listPanel =
                showSingleSelectParticipantList(wir, "Delegate", getSubordinatesOf());
        if (listPanel != null) {
            listPanel.addOKListener(e -> {
                getContentPanel().remove(listPanel);

                try {
                    Participant pTo = listPanel.getSelected();
                    if (pTo != null) {
                        getResourceClient().delegateItem(wir.getID(), getParticipantID(),
                                pTo.getID());
                        refresh();
                        Announcement.success("Delegated item '%s' to %s",
                                wir.getID(), pTo.getFullName());
                    }
                }
                catch (IOException | ResourceGatewayException ex) {
                    Announcement.error("Failed to delegate item: " + ex.getMessage());
                }
            });
        }
    }


    private void acceptAndStart(WorkItemRecord wir) {
        if (! checkItemStatus(wir)) return;

        performAction(Action.Accept, wir);

        // only start it next if the item is not already set to system start
        if (getQueueSet().hasWorkItemInQueue(wir.getID(), WorkQueue.ALLOCATED)) {
            startItem(wir, getParticipantID());
        }
    }



    private void edit(WorkItemRecord wir) {
        if (! checkItemStatus(wir)) return;

        try {
            if (! StringUtil.isNullOrEmpty(wir.getCustomFormURL())) {
                String caseData = getResourceClient().getCaseData(wir.getRootCaseID());

                // if the custom form launches, we're done, otherwise default to dyn form
                if (_customFormLauncher.show(wir, caseData, getParticipantID())) {
                    return;
                }
                Announcement.warn("Missing or invalid custom form, defaulting to dynamic form");
            }
            String schema = getResourceClient().getWorkItemDataSchema(wir.getID());
            DynForm dynForm = new DynForm(getParticipant(), wir, schema);
            dynForm.addOkListener(e -> {
                if (dynForm.validate()) {                                // completion
                    String outputData = dynForm.generateOutputData();
                    completeItem(wir, outputData);
                    dynForm.close();
                    refresh();
                }
            });
            dynForm.addSaveListener(e -> {
                if (dynForm.validate()) {                                // save
                    String outputData = dynForm.generateOutputData();
                    saveWorkItemData(wir, outputData);
                    dynForm.close();
                    refresh();
                }
            });

            dynForm.open();
        }
        catch (ResourceGatewayException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void completeItem(WorkItemRecord wir, String data) {
        if (! checkItemStatus(wir)) return;

        try {
            getResourceClient().completeItem(wir, data, getParticipantID());
            Announcement.success("Work item %s completed", wir.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to complete work item: " + e.getMessage());
        }
    }

    
    private void saveWorkItemData(WorkItemRecord wir, String data) {
        try {
            getResourceClient().updateWorkItemData(wir.getID(), data);
            Announcement.success("Work item %s saved", wir.getID());
        }
        catch (IOException  | ResourceGatewayException e) {
            Announcement.error("Failed to save data to work item: " + e.getMessage());
        }
    }


    private ContextMenu addContextMenu(ActionRibbon ribbon) {
        ActionIcon icon = ribbon.add(VaadinIcon.MENU,"Other Actions", null);
        return createContextMenu(icon);
    }


    private ContextMenu createContextMenu(ActionIcon icon) {
        ContextMenu menu = new ContextMenu(icon);
        menu.setOpenOnClick(true);
        return menu;
    }


    private TaskPrivileges getTaskPrivileges(WorkItemRecord wir) {
        try {
            return getResourceClient().getTaskPrivileges(wir);
        }
        catch (IOException | ResourceGatewayException e) {
            e.printStackTrace();
            return null;
        }
    }


    private Element getItemData(WorkItemRecord wir) {
        Element data = wir.getUpdatedData();
        return data != null ? data : wir.getDataList();
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


    private boolean userMayEdit(WorkItemRecord wir) {
        return ! (wir.getDataList() == null && wir.getCustomFormURL() == null);
    }


    private boolean userMayComplete(WorkItemRecord wir) {
        return wir.getDataList() == null || wir.getUpdatedData() != null;
    }


    private boolean userMayAddNewInstance(WorkItemRecord wir) {
        try {
            return wir.isDynamicCreationAllowed() &&
                    getEngineClient().canCreateNewInstance(wir.getID());
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
