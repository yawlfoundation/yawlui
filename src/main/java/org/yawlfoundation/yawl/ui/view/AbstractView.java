package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public abstract class AbstractView extends VerticalLayout {

    private final ResourceClient _resClient;
    private final EngineClient _engClient;


    public AbstractView(ResourceClient resClient, EngineClient engClient) {
        _resClient = resClient;
        _engClient = engClient;
    }


    abstract Component createLayout();

    
    protected ResourceClient getResourceClient() { return _resClient; }

    protected EngineClient getEngineClient() { return _engClient; }


    protected SplitLayout createSplitView(Component top, Component bottom) {
        SplitLayout splitLayout = new SplitLayout(top, bottom);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSizeFull();
        splitLayout.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        return splitLayout;
    }


    protected void refreshHeader(H4 header, String text, int count) {
        header.getElement().setText(String.format("%s (%d)", text, count));
    }


    protected void announceError(String msg) {
        Announcement.error(StringUtil.unwrap(msg));
    }
    
}
