package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.tabs.Tab;

import java.util.List;

/**
 * @author Michael Adams
 * @date 5/9/2022
 */
public class NonHumanResourcesView extends AbstractTabbedView {

    private Tab _tabResources;
    private Tab _tabCategories;


    public NonHumanResourcesView() {
        super(null);
    }


    @Override
    protected List<Tab> getTabs() {
        if (_tabResources == null) _tabResources = new Tab("Resources");
        if (_tabCategories == null) _tabCategories = new Tab("Categories");
        return List.of(_tabResources, _tabCategories);
    }


    @Override
    protected void setContent(Tab tab) {
        if (tab.equals(_tabResources)) {
            content.add(new NonHumanResourceSubView());
        }
        else {
            content.add(new NonHumanCategorySubView());
        }
    }

}
