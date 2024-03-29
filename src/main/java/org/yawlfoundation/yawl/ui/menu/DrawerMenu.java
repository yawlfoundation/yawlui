package org.yawlfoundation.yawl.ui.menu;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.util.InstalledServices;

/**
 * @author Michael Adams
 * @date 3/11/20
 */
public class DrawerMenu extends Tabs {

    private Tab initialItem;


    public DrawerMenu(Participant p) {
        setOrientation(Orientation.VERTICAL);
        addItems(p);
    }



    public void insertWorkletItem() {
        int insertionIndex = getComponentCount() - 2;
        this.addComponentAtIndex(insertionIndex,
                createTab(VaadinIcon.WRENCH, "Worklet Admin"));
    }


    public void removeWorkletItem() {
        getChildren().filter(tab -> ((Tab) tab).getLabel().equals("Worklet Admin"))
                .findFirst().ifPresent(this::remove);
    }


    private void addItems(Participant p) {

        // normal user
        if (p != null) {
            initialItem = createTab(VaadinIcon.INBOX, "My Worklist");
            add(initialItem);
            add(createTab(VaadinIcon.USER_CHECK, "My Profile"));
        }

        // quasi-admin privileges
        if (canViewTeamQueues(p)) {
            add(createTab(VaadinIcon.CLIPBOARD_USER, "My Team's Worklist"));
        }
        if (canManageCases(p)) {
            add(createTab(VaadinIcon.AUTOMATION, "Case Mgt"));
        }

        if (isAdmin(p)) {
            initialItem = createTab(VaadinIcon.RECORDS, "Admin Worklist");
            add(initialItem);
            add(createTab(VaadinIcon.USER, "Participants"));
            add(createTab(VaadinIcon.GROUP, "Org Data"));
            add(createTab(VaadinIcon.CLUSTER, "Non-Human Resources"));
            add(createTab(VaadinIcon.LINK, "Services / Clients"));
            add(createTab(VaadinIcon.CALENDAR_CLOCK, "Calendar"));
            if (new InstalledServices().hasWorkletService()) {
                add(createTab(VaadinIcon.WRENCH, "Worklet Admin"));
            }
        }
        add(createTab(VaadinIcon.QUESTION_CIRCLE_O, "About"));
        add(createTab(VaadinIcon.SIGN_OUT_ALT, "Logout"));
    }


    private Tab createTab(VaadinIcon vi, String label) {
        HorizontalLayout layout = new HorizontalLayout();
        Icon icon = new Icon(vi);
        icon.setSize("12px");
        Span span = new Span(label);
        layout.add(icon, span);
        Tab tab = new Tab(layout);
        tab.setLabel(label);
        return tab;
    }


    private boolean isAdmin(Participant p) {
        return p == null || p.isAdministrator();
    }


    // a participant with the relevant privileges (but not the 'admin' user)
    private boolean canViewTeamQueues(Participant p) {
        return p != null && (p.isAdministrator() ||
                        p.getUserPrivileges().canViewOrgGroupItems() ||
                        p.getUserPrivileges().canViewTeamItems());
    }

    
    private boolean canManageCases(Participant p) {
        return isAdmin(p) || p.getUserPrivileges().canManageCases();
    }


    public void selectInitialItem() {
        setSelectedTab(null);
        setSelectedTab(initialItem);
    }

}
