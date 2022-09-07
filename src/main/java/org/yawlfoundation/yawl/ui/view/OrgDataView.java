package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

/**
 * @author Michael Adams
 * @date 5/9/2022
 */
public class OrgDataView extends AbstractView {

    private final Tab _tabRoles = new Tab("Roles");
    private final Tab _tabCapabilities = new Tab("Capabilities");
    private final Tab _tabPositions = new Tab("Positions");
    private final Tab _tabOrgGroups = new Tab("Org Groups");
    private final VerticalLayout content = new VerticalLayout();


    public OrgDataView(ResourceClient resClient) {
        super(resClient, null);
        add(createLayout());
        setSizeFull();
    }

    @Override
    Component createLayout() {
        Tabs tabs = new Tabs(_tabRoles, _tabCapabilities, _tabPositions, _tabOrgGroups);
        tabs.addSelectedChangeListener(event -> setContent(event.getSelectedTab()));

        content.setSpacing(false);
        content.setSizeFull();
        setContent(tabs.getSelectedTab());
        return new VerticalLayout(tabs, content);
    }


    private void setContent(Tab tab) {
        content.removeAll();
        if (tab.equals(_tabRoles)) {
            content.add(new Paragraph("This is the Roles tab"));
        }
        else if (tab.equals(_tabCapabilities)) {
            content.add(new Paragraph("This is the Caps tab"));
        }
        else if (tab.equals(_tabPositions)) {
            content.add(new Paragraph("This is the Positions tab"));
        }
        else {
            content.add(new Paragraph("This is the Org Groups tab"));
        }
    }

}
