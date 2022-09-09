package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 9/8/2022
 */
public class SingleSelectParticipantList extends AbstractParticipantList {

    private  ListBox<Participant> listbox;


    public SingleSelectParticipantList(List<Participant> pList, String title) {
        super(pList, title);
    }

    public SingleSelectParticipantList(List<Participant> pList, String title,
                                       boolean showRibbon) {
        super(pList, title, showRibbon);
    }


    public String getSelectedID() {
        Participant p = listbox.getValue();
        return p != null ? p.getID() : null;
    }


    public Participant getSelected() {
        return listbox.getValue();
    }

    public void setSelected(Participant selected) { listbox.setValue(selected); }


    @Override
    protected Set<Participant> getSelection() {
        Participant p = getSelected();
        if (p != null) {
            Set<Participant> pSet = new HashSet<>();
            pSet.add(p);
            return pSet;
        }
        return Collections.emptySet();
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
