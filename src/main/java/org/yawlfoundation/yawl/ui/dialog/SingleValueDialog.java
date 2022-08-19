package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.TextField;

/**
 * @author Michael Adams
 * @date 18/8/2022
 */
public class SingleValueDialog extends AbstractDialog {

    private final TextField _field = new TextField();
    private Button _okButton;

    public SingleValueDialog() { }

    public SingleValueDialog(String title, String text) {
        super(title, text);
        addComponent(_field);
        createButtons();
    }

    public void setPrompt(String prompt) {
        _field.setLabel(prompt);
    }


    public Button getOKButton() { return _okButton; }


    public String getValue() {
        return _field.getValue();
    }

    private Button createButtons() {
        Button cancel = new Button("Cancel", event -> close());
        _okButton = new Button("OK");                 // listener added later
        _okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(cancel, _okButton);
        return _okButton;
    }

}
