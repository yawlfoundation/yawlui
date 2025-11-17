package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.util.ParticipantFieldTransposer;

/**
 * @author Michael Adams
 * @date 7/11/2025
 */
public class ResourceNameSelector extends UnpaddedVerticalLayout {

    private RadioButtonGroup<ParticipantFieldTransposer.Field> _choices;

    public ResourceNameSelector() {
        createLayout();
        add(_choices);
        setWidth("220px");
    }

    public void createLayout() {
        _choices = new RadioButtonGroup<>();
        _choices.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        _choices.setLabel("Show resources as:");
        _choices.setItemLabelGenerator(ParticipantFieldTransposer.Field::getLabel);
        _choices.setItems(ParticipantFieldTransposer.Field.values());
        _choices.setValue(ParticipantFieldTransposer.Field.Key);    // default
    }


    public ParticipantFieldTransposer.Field getSelection() {
        return _choices.getValue();
    }


    public void setSelection(ParticipantFieldTransposer.Field field) {
        _choices.setValue(field);
    }

}
