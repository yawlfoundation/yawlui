package org.yawlfoundation.yawl.ui.dialog.worklet;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;

import java.util.List;

/**
 * @author Michael Adams
 * @date 22/11/2022
 */
public abstract class AbstractWorkletDialog extends AbstractDialog {

    private final TextField _titleField = new TextField("Title");
    private final TextArea _scenarioArea = new TextArea("Description");
    private final TextArea _processArea = new TextArea("Suggested Process");

    private final Button ok = new Button("OK");


    public AbstractWorkletDialog(String title, List<String> triggers) {
        super(title);
        addComponent(createContent(triggers));
        createButtons(ok);
    }

    abstract VerticalLayout createContent(List<String> triggers);


    public Button getOKButton() { return ok; }


    public boolean validate() {
        return validateFields();
    }


    public String getHeading() { return _titleField.getValue(); }

    public String getScenario() { return _scenarioArea.getValue(); }

    public String getProcess() { return _processArea.getValue(); }


    protected VerticalLayout layoutTextFields() {
        _titleField.setWidthFull();
        _scenarioArea.setWidthFull();
        _processArea.setWidthFull();

        _titleField.setRequiredIndicatorVisible(true);
        _scenarioArea.setRequiredIndicatorVisible(true);
        
        return new VerticalLayout(_titleField, _scenarioArea, _processArea);
    }


    protected boolean validateFields() {
        _titleField.setInvalid(false);
        if (_titleField.isEmpty()) {
            _titleField.setErrorMessage("A title is required");
            _titleField.setInvalid(true);
        }
        _scenarioArea.setInvalid(false);
        if (_scenarioArea.isEmpty()) {
            _scenarioArea.setErrorMessage("A description is required");
            _scenarioArea.setInvalid(true);
        }
        return ! (_titleField.isInvalid() || _scenarioArea.isInvalid());
    }

}
