package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class ServicesView extends AbstractView {

    public ServicesView() {
        super();
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        return createSplitView(
                new ServicesSubView().createLayout(),
                new ClientAppSubView().createLayout()
        );
    }
    
}
