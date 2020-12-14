package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

/**
 * @author Michael Adams
 * @date 3/11/20
 */
public class DrawerMenu extends Tabs {

    public DrawerMenu(Participant p) {
        setOrientation(Orientation.VERTICAL);
        addItems(p);
    }


    private void addItems(Participant p) {
        if (p != null) {                                                 // normal user
            add(createTab(VaadinIcon.INBOX, "My Worklist"));
            add(createTab(VaadinIcon.USER_CHECK, "My Profile"));
        }

        // show quasi-admin privileges
        if (canViewTeamQueues(p)) {
            add(createTab(VaadinIcon.CLIPBOARD_USER, "My Team's Worklist"));
        }
        if (canManageCases(p)) {
            add(createTab(VaadinIcon.AUTOMATION, "Case Mgt"));
        }

        if (isAdmin(p)) {              
            add(createTab(VaadinIcon.RECORDS, "Admin Worklist"));
            add(createTab(VaadinIcon.USER, "Resources"));
            add(createTab(VaadinIcon.GROUP, "Org Data"));
            add(createTab(VaadinIcon.CLUSTER, "Non-Human Resources"));
            add(createTab(VaadinIcon.CALENDAR_CLOCK, "Calendar"));
            add(createTab(VaadinIcon.LINK, "Clients"));
        }
        add(createTab(VaadinIcon.QUESTION_CIRCLE_O, "About"));
        add(createTab(VaadinIcon.EXIT, "Exit"));
    }


    private Tab createTab(VaadinIcon vi, String label) {
        HorizontalLayout layout = new HorizontalLayout();
        Icon icon = new Icon(vi);
        icon.setSize("24px");
        Span span = new Span(label);
//        span.getStyle().set("fontSize", "75%");
        layout.add(icon, span);
        return new Tab(layout);
    }


    private boolean isAdmin(Participant p) {
        return p == null || p.isAdministrator();
    }


    private boolean canViewTeamQueues(Participant p) {
        return p != null && (p.isAdministrator() ||
                p.getUserPrivileges().canViewOrgGroupItems() ||
               p.getUserPrivileges().canViewTeamItems());
    }

    
    private boolean canManageCases(Participant p) {
        return isAdmin(p) || p.getUserPrivileges().canManageCases();
    }

}
