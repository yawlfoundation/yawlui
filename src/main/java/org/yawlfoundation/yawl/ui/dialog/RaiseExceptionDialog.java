package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

/**
 * @author Michael Adams
 * @date 22/11/2022
 */
public class RaiseExceptionDialog extends AbstractDialog {

    private final TextField _titleField = new TextField("Title");
    private final TextArea _scenarioArea = new TextArea("Description");
    private final TextArea _processArea = new TextArea("Suggested Process");

    private final Button ok = new Button("Raise");
    private final RadioButtonGroup<String> _radios = new RadioButtonGroup<>();

    private final String NEW_TRIGGER = "New External Exception Type...";


    public RaiseExceptionDialog(String title, List<String>triggers) {
        super(title);
        addComponent(createContent(triggers));
        createButtons(ok);
    }


    public Button getOKButton() { return ok; }


    public boolean validate() {
        return ! isNewException() || validateNewFields();
    }


    public boolean isNewException() { return _radios.getValue().equals(NEW_TRIGGER); }


    public String getSelection() {
        String selection = _radios.getValue();
        return selection != null ? selection : "None";
    }


    public String getNewTitle() { return _titleField.getValue(); }

    public String getNewScenario() { return _scenarioArea.getValue(); }

    public String getNewProcess() { return _processArea.getValue(); }


    private VerticalLayout createContent(List<String>triggers) {
        _titleField.setWidthFull();
        _scenarioArea.setWidthFull();
        _processArea.setWidthFull();

        _titleField.setRequiredIndicatorVisible(true);
        _scenarioArea.setRequiredIndicatorVisible(true);
        _processArea.setRequiredIndicatorVisible(true);

        VerticalLayout subDialog = createSubDialog();
        subDialog.setVisible(false);

        _radios.setLabel("Select the exception that has occurred");
        triggers.add(NEW_TRIGGER);
        _radios.setItems(triggers);
        _radios.addValueChangeListener(event ->
            subDialog.setVisible(event.getValue().equals(NEW_TRIGGER)));

        return new VerticalLayout(_radios, subDialog);
    }


    private VerticalLayout createSubDialog() {
        return new VerticalLayout(_titleField, _scenarioArea, _processArea);
    }


    private boolean validateNewFields() {
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
        _processArea.setInvalid(false);
        if (_processArea.isEmpty()) {
            _processArea.setErrorMessage("A suggested process is required");
            _processArea.setInvalid(true);
        }
        return ! (_titleField.isInvalid() || _scenarioArea.isInvalid() ||
                _processArea.isInvalid());
    }

}
