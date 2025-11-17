package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.ui.component.ResourceNameSelector;
import org.yawlfoundation.yawl.ui.component.SingleSelectSpecificationIdList;
import org.yawlfoundation.yawl.ui.util.ParticipantFieldTransposer;

import java.util.List;

/**
 * @author Michael Adams
 * @date 3/11/2025
 */
public class SpecificationIdDialog extends AbstractDialog {

    private final SingleSelectSpecificationIdList _specList;
    private final Button _ok = new Button("OK", e -> close());

    private ResourceNameSelector _nameSelector;


    public SpecificationIdDialog(List<YSpecificationID> items,
                                 SingleSelectSpecificationIdList.Versions versions,
                                 boolean showResourceNameSelector) {
        super("Select Specification");
        _specList = new SingleSelectSpecificationIdList(items, versions);
        _specList.setHeight("220px");
        _specList.addValueChangeListener(
                event -> _ok.setEnabled(true));

        if (showResourceNameSelector) {
            _nameSelector = new ResourceNameSelector();
            addComponent(new HorizontalLayout(_specList, _nameSelector));
            setWidth("750px");
        }
        else {
            addComponent(_specList);
        }
         createButtons(_ok);
        _ok.setEnabled(false);
     }



    public void addOkClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _ok.addClickListener(listener);
    }


    public YSpecificationID getSelected() { return _specList.getValue(); }

    public ParticipantFieldTransposer.Field getSelectedResourceNameFormat() {
        return _nameSelector != null ? _nameSelector.getSelection() :
                ParticipantFieldTransposer.Field.Key;
    }
    
}
