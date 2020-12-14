package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class MessageDialog extends Dialog {

    public MessageDialog(String msg) {
        setCloseOnOutsideClick(false);
        setWidth("400px");
        setHeight("400px");
        VerticalLayout vl = new VerticalLayout();
        vl.add(new Text(msg), new Button("OK", event -> {
            close();
        }));
        add(vl);
    }
}
