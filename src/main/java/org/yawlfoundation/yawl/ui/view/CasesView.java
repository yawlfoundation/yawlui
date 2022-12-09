package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public class CasesView extends AbstractView {



    public CasesView() {
        super();
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        return createSplitView(
                new SpecificationsSubView().createLayout(),
                new CasesSubView().createLayout()
        );
    }

}
