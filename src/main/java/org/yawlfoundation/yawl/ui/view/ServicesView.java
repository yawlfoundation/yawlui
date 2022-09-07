package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class ServicesView extends AbstractView {

    public ServicesView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        return createSplitView(
                new ServicesSubView(getResourceClient(), getEngineClient()).createLayout(),
                new ClientAppSubView(getResourceClient(), getEngineClient()).createLayout()
        );
    }
    
}
