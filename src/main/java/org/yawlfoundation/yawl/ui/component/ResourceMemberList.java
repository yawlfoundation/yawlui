package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;

import java.util.List;

/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class ResourceMemberList extends UnpaddedVerticalLayout {

    private ActionIcon removeAction;
    private ActionIcon addAction;
    private Prompt heading;
    private final ListBox<Participant> _membersList = new ListBox<>();


    public ResourceMemberList(List<Participant> items) {
        super();
        setPadding(false);
        setSpacing(false);
        
        add(createTitle(items));
        add(createList(items));
        add(createRibbon());
        setWidth("fit-content");
        setHeight("225px");
    }


    public Participant getSelected() { return _membersList.getValue(); }


    public void addAddButtonListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        addAction.addClickListener(listener);
    }


    public void addRemovebuttonListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        removeAction.addClickListener(listener);
    }


    public void refresh(List<Participant> items) {
        _membersList.setItems(items);
        _membersList.getListDataView().refreshAll();
        heading.setText(buildLabelText(items));
    }


    protected Label createTitle(List<Participant> items) {
        heading = new Prompt(buildLabelText(items));
        return heading;
    }


    protected ListBox<Participant> createList(List<Participant> items) {
        _membersList.setItems(items);
        _membersList.setItemLabelGenerator(Participant::getFullName);
        _membersList.setWidth("210px");
        _membersList.getElement().getStyle().set("overflow-y", "scroll");   
        _membersList.getElement().getStyle().set("flex-grow", "1");
        addDivider(_membersList);
        return _membersList;
    }


    protected ActionRibbon createRibbon() {
        removeAction = new ActionIcon(VaadinIcon.CLOSE,
                ActionIcon.RED, "Remove", null);
        removeAction.setSize("12px");
        addAction = new ActionIcon(VaadinIcon.PLUS, null,
                 "Add", null);
        addAction.setSize("12px");

        ActionRibbon ribbon = new ActionRibbon();
        ribbon.setJustifyContentMode(JustifyContentMode.END);
        ribbon.add(removeAction, addAction);
        ribbon.setSpacing(false);
        ribbon.setWidthFull();
        ribbon.getElement().getStyle().set("padding", "5px");
        addDivider(ribbon);
        return ribbon;
    }

    
    private String buildLabelText(List<Participant> items) {
        return String.format("Members (%d)", items.size());
    }


    private void addDivider(Component component) {
        component.getElement().getStyle().set("border-left", "3px solid lightgray");
    }

}
