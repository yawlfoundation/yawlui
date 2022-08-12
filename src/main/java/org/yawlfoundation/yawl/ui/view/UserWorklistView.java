package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class UserWorklistView extends AbstractWorklistView {

    public UserWorklistView(ResourceClient client) {
        super(client);
    }

    @Override
    protected QueueSet getQueueSet() {
        return null;
    }

    @Override
    protected String getTitle() {
        return "My Work List";
    }

    @Override
    protected ActionRibbon createColumnActions(WorkItemRecord wir) {
        ActionRibbon ribbon = new ActionRibbon();
        switch(wir.getResourceStatus()) {
            case "Offered" : {
                ribbon.add(VaadinIcon.TAB_A, "#A37063", "Accept",
                        e -> accept(wir));
                ActionIcon icon =
                ribbon.add(VaadinIcon.MENU, "#A37063", "Other Actions", null);
                ContextMenu menu = new ContextMenu(icon);
                menu.setOpenOnClick(true);
                menu.addItem("Start");
                menu.addItem("Chain");
                break;
            }
            case "Allocated" : {
                ribbon.add(VaadinIcon.CARET_RIGHT, "#009926", "Start",
                        event -> start(wir));
                break;
            }
            case "Started" : {
                 ribbon.add(VaadinIcon.PENCIL, "#009926", "View/Edit",
                         event -> edit(wir));
                 break;
            }
            case "Suspended" : {
                 ribbon.add(VaadinIcon.TIME_BACKWARD, "#009926", "Start",
                         event -> unsuspend(wir));
                 break;
            }
        }
        return ribbon;
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
        return null;
    }



}
