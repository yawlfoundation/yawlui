package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.util.UiUtil;

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
        Participant p = listbox.getValue();
        return p != null ? p.getID() : null;
    }


    public Participant getSelected() {
        return listbox.getValue();
    }


    @Override
    Component createListBox(List<Participant> pList) {
        listbox = new ListBox<>();
        listbox.setItems(pList);
        listbox.setItemLabelGenerator(Participant::getFullName);
        UiUtil.setStyle(listbox, "overflow-y", "scroll");
        UiUtil.setStyle(listbox, "flex-grow", "1");
        return listbox;
    }


    @Override
    void updateList(List<Participant> pList) {
        listbox.setItems(pList);
        listbox.getListDataView().refreshAll();
    }
}
