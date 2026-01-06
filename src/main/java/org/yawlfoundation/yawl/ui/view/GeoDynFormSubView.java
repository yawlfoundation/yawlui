package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.util.UiUtil;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class GeoDynFormSubView extends AbstractView {

    private final DynForm _form;

    public GeoDynFormSubView(DynForm form) {
        super();
        _form = form;
        add(createLayout());
        setSizeFull();
    }


    @Override
    Component createLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.add(buildHeader());
        layout.add(buildFormName());
        layout.add(_form.getContainingScroller());
        layout.add(buildButtonBar());
        layout.setSizeFull();
        return layout;
    }


    private H5 buildFormName() {
        H5 name = new H5(_form.getFormName());
        name.getStyle().set("margin-top", "0px");
        return name;
    }


    private H4 buildHeader() {
        H4 header = _form.getHeaderTextFormatted();
        UiUtil.setStyle(header, "margin-bottom", "10px");
        UiUtil.removeTopMargin(header);
        return header;
    }

    private JustifiedButtonLayout buildButtonBar() {
        JustifiedButtonLayout bar = _form.getButtonBar();
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        return bar;
    }
    
}
