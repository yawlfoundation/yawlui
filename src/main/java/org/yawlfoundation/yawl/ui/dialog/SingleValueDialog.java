package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;

/**
 * @author Michael Adams
 * @date 18/8/2022
 */
public class SingleValueDialog extends AbstractDialog {

    private final TextField _field = new TextField();
    private final Button _okButton = new Button("OK");;

    public SingleValueDialog() { }

    public SingleValueDialog(String title, String text) {
        super(title, text);
        addComponent(_field);
        createButtons(_okButton);
        setWidth("350px");
        _field.focus();
        _field.setWidthFull();
    }

    public void setPrompt(String prompt) {
        _field.setLabel(prompt);
    }


    public Button getOKButton() { return _okButton; }


    public String getValue() {
        return _field.getValue();
    }

}
