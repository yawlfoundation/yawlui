package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import org.yawlfoundation.yawl.ui.layout.VerticalScrollLayout;

/**
 * @author Michael Adams
 * @date 8/8/2022
 */
public class AdminWorklistOptionsDialog extends AbstractDialog {

    private final Button ok = new Button("OK");
    private final Button cancel = new Button("Cancel", e-> close());
    private final Checkbox cbxToMe = new Checkbox(
            "Send all actions directly to my worklist");


    public AdminWorklistOptionsDialog() {
        super("Admin Worklist Settings");
        addComponent(createPanel());
        addButtons();
    }


    public Button getOK() { return ok; }

    public boolean isChecked() { return ! cbxToMe.getValue(); }
    

    private VerticalScrollLayout createPanel() {
        VerticalScrollLayout layout = new VerticalScrollLayout();
        layout.add(cbxToMe);
        return layout;
    }


    private void addButtons() {
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(cancel, ok);
    }

}
