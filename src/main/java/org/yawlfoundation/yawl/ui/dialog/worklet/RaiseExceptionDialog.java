package org.yawlfoundation.yawl.ui.dialog.worklet;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import java.util.List;

/**
 * @author Michael Adams
 * @date 22/11/2022
 */
public class RaiseExceptionDialog extends AbstractWorkletDialog {

    private RadioButtonGroup<String> _radios;
    private final String NEW_TRIGGER = "New External Exception Type...";


    public RaiseExceptionDialog(String title, List<String>triggers) {
        super(title, triggers);
        setText("Please describe why the exception should be raised");
    }


    public boolean isNewException() { return _radios.getValue().equals(NEW_TRIGGER); }


    public String getSelection() {
        String selection = _radios.getValue();
        return selection != null ? selection : "None";
    }


    protected VerticalLayout createContent(List<String>triggers) {
        VerticalLayout subDialog = layoutTextFields();
        subDialog.setVisible(false);

        _radios = new RadioButtonGroup<>();
        _radios.setLabel("Select the exception that has occurred");
        triggers.add(NEW_TRIGGER);
        _radios.setItems(triggers);
        _radios.addValueChangeListener(event ->
            subDialog.setVisible(event.getValue().equals(NEW_TRIGGER)));

        return new VerticalLayout(_radios, subDialog);
    }

}
