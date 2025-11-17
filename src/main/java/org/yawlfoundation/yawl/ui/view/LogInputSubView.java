package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.util.UiUtil;

/**
 * @author Michael Adams
 * @date 24/10/2025
 */
public class LogInputSubView extends AbstractView {

    private final LogView _parent;
    private final Div _descriptionPanel = new Div();
    private final ListBox<LogViewType> _logTypeList = new ListBox<>();
    private final Button _btnGo = new Button("Select...");

    
    public LogInputSubView(LogView parent) {
        super();
        _parent = parent;
        _descriptionPanel.setText(" ");
        setWidth("900px");
    }


    protected LogViewType getSelectedLog() {
        return _logTypeList.getValue();
    }


    @Override
    Component createLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setHeight("240px");
        H4 header = createHeader("Log Type");
        header.getStyle().set("margin-bottom", "0");
        layout.add(header);
        layout.add(createForm());
        UiUtil.removeTopMargin(layout);
        layout.setPadding(false);
        return layout;
    }

    
    private FormLayout createForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.add(createLogTypePanel(), 1);
        form.add(createRightPanel(), 2);
        form.setWidthFull();
        return form;
    }


    private VerticalLayout createLogTypePanel() {
        prepareListBox();
        VerticalLayout layout = new VerticalLayout();  // inside VL for correct spacing
        layout.add(_logTypeList);
        layout.setPadding(false);
        layout.setMargin(false);
        return layout;
    }


    private VerticalLayout createRightPanel() {
        VerticalLayout layout = new UnpaddedVerticalLayout();
        _descriptionPanel.setWidthFull();
        _descriptionPanel.getStyle().set("margin-top", "0");
        _descriptionPanel.getStyle().set("padding-bottom", "0");
        _descriptionPanel.getStyle().set("font-size", "smaller");
        layout.add(_descriptionPanel);
        layout.add(createButtonBar());
        return layout;
    }


    private HorizontalLayout createButtonBar() {
        _btnGo.addClickListener( e -> _parent.generateOutputView(this));
        _btnGo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        _btnGo.setEnabled(false);

        HorizontalLayout layout = new HorizontalLayout(_btnGo);
        layout.setJustifyContentMode(JustifyContentMode.END);
        return layout;
    }


    private void prepareListBox() {
        _logTypeList.setItems(LogViewType.values());
        _logTypeList.setItemLabelGenerator(LogViewType::shortText);
        _logTypeList.setHeight("135px");
        _logTypeList.setWidthFull();
        _logTypeList.addValueChangeListener(e -> {
            _descriptionPanel.setText(_logTypeList.getValue().description());
            _btnGo.setEnabled(true);
        });
        _logTypeList.getElement().getStyle().set("overflow-y", "scroll");
        _logTypeList.getElement().getStyle().set("flex-grow", "1");
        _logTypeList.getElement().getStyle().set("border", "1px solid lightgray");
    }
    
}
