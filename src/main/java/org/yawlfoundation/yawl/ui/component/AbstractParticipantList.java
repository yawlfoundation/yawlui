package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H5;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 9/8/2022
 */
public abstract class AbstractParticipantList extends UnpaddedVerticalLayout {

    private final Button ok = new Button("OK");
    private final Button cancel = new Button("Cancel");

    private final List<Participant> _fullList;
    private List<Participant> _filteredList;

    public AbstractParticipantList(List<Participant> pList, String action, String itemID) {
        _fullList = pList;
        add(new H5(String.format("%s Work Item '%s'", action, itemID)));
        add(createListBox(sortByName(pList)));
        add(createButtons());
        setWidth("300px");
    }

    abstract Component createListBox(List<Participant> pList);


    public void addOKListener(ComponentEventListener<ClickEvent<Button>> listener) {
        ok.addClickListener(listener);
    }


    public void addCancelListener(ComponentEventListener<ClickEvent<Button>> listener) {
        cancel.addClickListener(listener);
    }


    protected List<Participant> filterList(List<Participant> fullList, String chars) {
        List<Participant> filtered = new ArrayList<>();
        fullList.forEach(p -> {
            if (p.getFullName().contains(chars)) {
                filtered.add(p);
            }
        });
        return filtered;
    }

    
    private JustifiedButtonLayout createButtons() {
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new JustifiedButtonLayout(cancel, ok);
     }


    private List<Participant> sortByName(List<Participant> list) {
        return list.stream().sorted(Comparator.comparing(
                                p -> ((Participant) p).getLastName().toLowerCase())
                        .thenComparing(
                                p -> ((Participant) p).getFirstName().toLowerCase()))
                .collect(Collectors.toList());
    }



}
