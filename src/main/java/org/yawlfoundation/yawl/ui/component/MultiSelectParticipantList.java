package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 9/8/2022
 */
public class MultiSelectParticipantList extends AbstractParticipantList {

    private MultiSelectListBox<Participant> listbox;
    private final Set<Participant> storedSelections = new HashSet<>();
    private boolean updating = false;

    public MultiSelectParticipantList(List<Participant> pList, String title) {
        super(pList, title);
    }

    public MultiSelectParticipantList(List<Participant> pList, String title,
                                       boolean showRibbon) {
        super(pList, title, showRibbon);
    }


    public Set<String> getSelectedIDs() {
        Set<String> ids = new HashSet<>();
        listbox.getValue().forEach(p -> ids.add(p.getID()));
        return ids;
    }


    public void setSelected(List<Participant> selected) {
        listbox.select(selected);
        storedSelections.addAll(selected);
    }


    public MultiSelectListBox<Participant> getListbox() {
        return listbox;
    }

    @Override
    public Set<Participant> getSelection() {
        return storedSelections;
    }


    @Override
    Component createListBox(List<Participant> pList) {
        listbox = new MultiSelectListBox<>();
        listbox.setItems(pList);
        listbox.setItemLabelGenerator(Participant::getFullName);
        UiUtil.setStyle(listbox, "overflow-y", "scroll");
        UiUtil.setStyle(listbox, "flex-grow", "1");

        listbox.addSelectionListener(e -> {
            if (! updating) {
                storedSelections.addAll(e.getAddedSelection());
                storedSelections.removeAll(e.getRemovedSelection());
            }
        });
        return listbox;
    }


    @Override
    void updateList(List<Participant> pList) {
        updating = true;
        listbox.setItems(pList);
        listbox.getListDataView().refreshAll();
        listbox.select(storedSelections);
        updating = false;
    }
    
}
