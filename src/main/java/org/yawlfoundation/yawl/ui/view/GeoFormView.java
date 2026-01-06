package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import org.yawlfoundation.yawl.ui.dynform.DynForm;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class GeoFormView extends AbstractView {

    private final DynForm _form;

    public GeoFormView(DynForm dynForm) {
        super();
        _form = dynForm;
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        GeoMapSubView mapSubView = new GeoMapSubView(_form);
        SplitLayout layout = createSplitView(
                new GeoDynFormSubView(_form).createLayout(),
                mapSubView.createLayout(),
                SplitLayout.Orientation.HORIZONTAL
        );
        layout.setSplitterPosition(25);

        layout.addSplitterDragendListener(e ->
                mapSubView.invalidateSize());

        return layout;
    }
    
}
