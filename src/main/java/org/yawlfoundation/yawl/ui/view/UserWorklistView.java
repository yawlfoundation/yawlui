package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class UserWorklistView extends AbstractWorklistView {


    public UserWorklistView(ResourceClient client, Participant p) {
        super(client, p);
    }

    @Override
    protected QueueSet getQueueSet(Participant p) {
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

    // todo user privs for items
    @Override
    protected ActionRibbon createColumnActions(WorkItemRecord wir) {
        ActionRibbon ribbon = new ActionRibbon();
        switch(wir.getResourceStatus()) {
            case "Offered" : {
                ribbon.add(VaadinIcon.TAB_A, "#A37063", "Accept",
                        e -> accept(wir));
                ContextMenu menu = addContextMenu(ribbon);
                menu.addItem("Start", e -> acceptAndStart(wir));
                menu.addItem("Chain", e -> chain(wir));
                break;
            }
            case "Allocated" : {
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Start",
                        event -> start(wir));
                ContextMenu menu = addContextMenu(ribbon);
                menu.addItem("Skip", e -> skip(wir));
                menu.addItem("Deallocate", e -> deallocate(wir));
                menu.addItem("Delegate", e -> delegate(wir));
                menu.addItem("Pile", e -> pile(wir));
                break;
            }
            case "Started" : {
                ribbon.add(VaadinIcon.PENCIL, "#009926", "View/Edit",
                        event -> edit(wir));
                ContextMenu menu = addContextMenu(ribbon);
                menu.addItem("Suspend", e -> suspend(wir));
                menu.addItem("Complete", e -> complete(wir));
                menu.addItem("Reallocate (stateful)", e -> reallocate(wir, true));
                menu.addItem("Reallocate (stateless)", e -> reallocate(wir, false));
                if (wir.isDynamicCreationAllowed()) {
                    menu.addItem("Add Instance", e -> newInstance(wir));
                }
                break;
            }
            case "Suspended" : {
                ribbon.add(VaadinIcon.TIME_BACKWARD, "#009926", "Unsuspend",
                        event -> unsuspend(wir));
                ribbon.add(VaadinIcon.MENU);
                break;
            }
        }
        return ribbon;
    }

    private void newInstance(WorkItemRecord wir) {

    }

    private void reallocate(WorkItemRecord wir, boolean b) {

    }

    private void complete(WorkItemRecord wir) {
    }

    private void suspend(WorkItemRecord wir) {

    }

    private void pile(WorkItemRecord wir) {
    }

    private void delegate(WorkItemRecord wir) {
    }

    private void deallocate(WorkItemRecord wir) {

    }

    private void skip(WorkItemRecord wir) {
    }

    private void chain(WorkItemRecord wir) {

    }

    private void acceptAndStart(WorkItemRecord wir) {

    }

    private void unsuspend(WorkItemRecord wir) {
        
    }

    private void edit(WorkItemRecord wir) {

    }

    private void start(WorkItemRecord wir) {

    }

    private void accept(WorkItemRecord wir) {

    }



    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "#0066FF", "Refresh", event ->
                refreshGrid());
        return ribbon;
    }


    private ContextMenu addContextMenu(ActionRibbon ribbon) {
        ActionIcon icon =
        ribbon.add(VaadinIcon.MENU, "#A37063", "Other Actions", null);
        ContextMenu menu = new ContextMenu(icon);
        menu.setOpenOnClick(true);
        return menu;
    }

}
