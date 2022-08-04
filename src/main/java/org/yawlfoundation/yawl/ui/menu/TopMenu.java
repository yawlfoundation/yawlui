package org.yawlfoundation.yawl.ui.menu;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

/**
 * @author Michael Adams
 * @date 3/11/20
 */
public class TopMenu extends FlexLayout {

    public TopMenu() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        Tabs tabs = new Tabs();
        tabs.add(createTab(VaadinIcon.INBOX, "Work List"));
        tabs.add(createTab(VaadinIcon.EDIT, "Clients"));
        tabs.add(createTab(VaadinIcon.USER, "Resources"));
        tabs.add(createTab(VaadinIcon.EXIT, "Exit"));
        add(tabs); 
    }


    public void addSelectionListener(ComponentEventListener<Tabs.SelectedChangeEvent> listener) {
        ((Tabs) getComponentAt(0) ).addSelectedChangeListener(listener);
    }


    private Tab createTab(VaadinIcon vi, String label) {
        VerticalLayout layout = new VerticalLayout();
        layout.getStyle().set("alignItems", "center");
        Icon icon = new Icon(vi);
        icon.setSize("24px");
        Span span = new Span(label);
        span.getStyle().set("fontSize", "75%");
        layout.add(icon, span);
        return new Tab(layout);
    }
}
