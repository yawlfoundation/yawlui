package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.List;

/**
 * @author Michael Adams
 * @date 9/8/2022
 */
public class SingleSelectParticipantList extends AbstractParticipantList {

    private  ListBox<Participant> listbox;


    public SingleSelectParticipantList(List<Participant> pList, String action, String itemID) {
        super(pList, action, itemID);
    }


    public String getSelectedID() {
        return listbox.getValue().getID();
    }


    public Participant getSelected() {
        return listbox.getValue();
    }


    @Override
    Component createListBox(List<Participant> pList) {
        listbox = new ListBox<>();
        listbox.setItems(pList);
        listbox.setItemLabelGenerator(Participant::getFullName);
        listbox.setHeight("80vh");
        return listbox;
    }


}
