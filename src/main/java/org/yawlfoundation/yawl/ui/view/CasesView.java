package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public class CasesView extends AbstractView {

    private final SpecificationsSubView _specsView;
    private final CasesSubView _casesView = new CasesSubView();


    public CasesView() {
        super();
        _specsView = new SpecificationsSubView(this);
        add(createLayout());
        _specsView.addDelayedCaseLaunchListener(_casesView);
        setSizeFull();
    }


    @Override
    Component createLayout() {
        return createSplitView(_specsView.createLayout(), _casesView.createLayout());
    }

}
