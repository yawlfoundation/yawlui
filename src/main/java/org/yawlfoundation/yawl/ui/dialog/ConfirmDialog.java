package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class ConfirmDialog extends Dialog {

    private Button _okButton;
    private Button _cancelButton;

    public ConfirmDialog(String msg) {
        super();
        setCloseOnOutsideClick(false);
        setWidth("400px");
        setHeight("150px");
        VerticalLayout vl = new VerticalLayout();
        vl.add(new Text(msg), createButtons());
        add(vl);
    }


    public void addOKClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _okButton.addClickListener(listener);
    }



    public void addCancelClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _cancelButton.addClickListener(listener);
    }


    private HorizontalLayout createButtons() {
        HorizontalLayout hl = new HorizontalLayout();
        Button _cancelButton = new Button("Cancel", event -> {
            close();
        });
        _okButton = new Button("OK", event -> {
            close();
        });
        hl.add(_cancelButton, _okButton);
        hl.setAlignItems(FlexComponent.Alignment.CENTER);
        return hl;
    }

}
