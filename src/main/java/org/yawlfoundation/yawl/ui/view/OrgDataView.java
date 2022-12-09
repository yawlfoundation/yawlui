package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.tabs.Tab;

import java.util.List;

/**
 * @author Michael Adams
 * @date 5/9/2022
 */
public class OrgDataView extends AbstractTabbedView {

    private Tab _tabRoles;
    private Tab _tabCapabilities;
    private Tab _tabPositions;
    private Tab _tabOrgGroups;


    public OrgDataView() {
        super(null);
    }


    @Override
    protected List<Tab> getTabs() {
        if (_tabRoles == null) _tabRoles = new Tab("Roles");
        if (_tabCapabilities == null) _tabCapabilities = new Tab("Capabilities");
        if (_tabPositions == null) _tabPositions = new Tab("Positions");
        if (_tabOrgGroups == null) _tabOrgGroups = new Tab("Org Groups");
        return List.of(_tabRoles, _tabCapabilities, _tabPositions, _tabOrgGroups);
    }


    @Override
    protected void setContent(Tab tab) {
        if (tab.equals(_tabRoles)) {
            content.add(new RoleSubView());
        }
        else if (tab.equals(_tabCapabilities)) {
            content.add(new CapabilitySubView());
        }
        else if (tab.equals(_tabPositions)) {
            content.add(new PositionSubView());
        }
        else {
            content.add(new OrgGroupSubView());
        }
    }

}
