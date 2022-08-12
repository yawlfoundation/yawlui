package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 9/8/2022
 */
public abstract class AbstractParticipantList extends VerticalLayout {

    private ActionIcon closeAction;
    private ActionIcon okAction;

    public AbstractParticipantList(List<Participant> pList, String action, String itemID) {
        add(new H4(String.format("%s Work Item '%s'", action, itemID)));
        add(createFilterField(pList));
        add(createListBox(sortByName(pList)));
        add(createRibbon());
        setHeightFull();
        setWidth("25%");
    }

    abstract Component createListBox(List<Participant> pList);

    abstract void updateList(List<Participant> pList);


    public void addOKListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        okAction.addClickListener(listener);
    }


    public void addCancelListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        closeAction.addClickListener(listener);
    }


    private ActionRibbon createRibbon() {
        closeAction = new ActionIcon(VaadinIcon.CLOSE,
                "red", "Cancel", null);
        okAction = new ActionIcon(VaadinIcon.CHECK,
                "green", "OK", null);
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.setJustifyContentMode(JustifyContentMode.END);
        ribbon.add(closeAction, okAction);
        ribbon.setWidthFull();
        return ribbon;
    }


    private HorizontalLayout createFilterField(List<Participant> fullList) {
        Icon icon = VaadinIcon.FILTER.create();
        icon.setSize("18px");
        icon.setColor("gray");

        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter");
        filterField.setPrefixComponent(icon);
        filterField.addValueChangeListener(e ->
                updateList(filterList(fullList, filterField.getValue())));
        filterField.setValueChangeMode(ValueChangeMode.EAGER);

        return new HorizontalLayout(filterField);
    }


    protected List<Participant> filterList(List<Participant> fullList, String chars) {
        if (chars == null || chars.isEmpty()) {
            return fullList;
        }
         List<Participant> filtered = new ArrayList<>();
         fullList.forEach(p -> {
             if (p.getFullName().toLowerCase().contains(chars.toLowerCase())) {
                 filtered.add(p);
             }
         });
         return filtered;
     }
     

    private List<Participant> sortByName(List<Participant> list) {
        return list.stream().sorted(Comparator.comparing(
                                p -> ((Participant) p).getLastName().toLowerCase())
                        .thenComparing(
                                p -> ((Participant) p).getFirstName().toLowerCase()))
                .collect(Collectors.toList());
    }
    
}
