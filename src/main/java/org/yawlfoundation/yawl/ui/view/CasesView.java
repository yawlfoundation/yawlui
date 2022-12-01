package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.service.WorkletClient;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public class CasesView extends AbstractView {



    public CasesView(ResourceClient resClient, EngineClient engClient, WorkletClient wsClient) {
        super(resClient, engClient, wsClient);
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        return createSplitView(
                new SpecificationsSubView(getResourceClient(),
                        getEngineClient()).createLayout(),
                new CasesSubView(getResourceClient(), getEngineClient(),
                        getWorkletClient()).createLayout()
        );
    }

}
