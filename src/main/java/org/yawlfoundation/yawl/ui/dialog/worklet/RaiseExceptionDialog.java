package org.yawlfoundation.yawl.ui.dialog.worklet;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;

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
    }


    public boolean isNewException() { return _radios.getValue().equals(NEW_TRIGGER); }


    public String getSelection() {
        String selection = _radios.getValue();
        return selection != null ? selection : "None";
    }


    protected VerticalLayout createContent(List<String>triggers) {
        triggers.add(NEW_TRIGGER);
        
        VerticalLayout subDialog = layoutTextFields();
        subDialog.setVisible(false);

        _radios = new RadioButtonGroup<>();
        _radios.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        _radios.setLabel("Select the exception that has occurred");
        _radios.setItems(triggers);
        _radios.addValueChangeListener(event ->
            subDialog.setVisible(event.getValue().equals(NEW_TRIGGER)));

        return new VerticalLayout(_radios, subDialog);
    }

}
