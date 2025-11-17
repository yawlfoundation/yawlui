package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.component.ResourceNameSelector;
import org.yawlfoundation.yawl.ui.component.SingleSelectParticipantList;
import org.yawlfoundation.yawl.ui.util.ParticipantFieldTransposer;

import java.util.List;

/**
 * @author Michael Adams
 * @date 3/11/2025
 */
public class SelectParticipantDialog extends AbstractDialog {

    private final SingleSelectParticipantList _pList;
    private final Button _ok = new Button("OK", e -> close());
    private ResourceNameSelector _nameSelector;


    public SelectParticipantDialog(List<Participant> items) {
        this(items, false);
    }


    public SelectParticipantDialog(List<Participant> items, boolean showResourceNameSelector) {
        super("Select Participant");
        setWidth("400px");
        _pList = new SingleSelectParticipantList(items, null, false);
        _pList.addClickListener(event -> _ok.setEnabled(true));
        _pList.setHeight("210px");
        _pList.setWidth("360px");

        if (showResourceNameSelector) {
            _nameSelector = new ResourceNameSelector();
            addComponent(new HorizontalLayout(_pList, _nameSelector));
            setWidth("650px");
        }
        else {
            addComponent(_pList);
        }
         createButtons(_ok);
        _ok.setEnabled(false);
     }

     
    public void addOkClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _ok.addClickListener(listener);
    }


    public Participant getSelected() { return _pList.getSelected(); }
    
    public ParticipantFieldTransposer.Field getSelectedResourceNameFormat() {
        return _nameSelector != null ? _nameSelector.getSelection() :
                ParticipantFieldTransposer.Field.Key;
    }

}
