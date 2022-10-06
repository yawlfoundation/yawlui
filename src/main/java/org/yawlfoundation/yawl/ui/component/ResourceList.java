package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;

import java.util.List;

/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class ResourceList<T> extends UnpaddedVerticalLayout {

    private ActionIcon removeAction;
    private ActionIcon addAction;
    private Prompt heading;
    private final String _label;
    private final ListBox<T> _listBox = new ListBox<>();


    public ResourceList(String label, List<T> items,
                        ItemLabelGenerator<T> labelGenerator) {
        super();
        _label = label;
        setPadding(false);
        setSpacing(false);
        
        add(createTitle(items));
        add(createList(items, labelGenerator));
        add(createRibbon());
        setWidth("fit-content");
        setHeight("225px");
    }


    public T getSelected() { return _listBox.getValue(); }

    public void select(T item) { _listBox.setValue(item); }


    public void addAddButtonListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        addAction.addClickListener(listener);
    }


    public void addRemoveButtonListener(ComponentEventListener<ClickEvent<Icon>> listener) {
        removeAction.addClickListener(listener);
    }


    public void addListSelectionListener(HasValue.ValueChangeListener
            <? super AbstractField.ComponentValueChangeEvent<ListBox<T>, T>> listener) {
        _listBox.addValueChangeListener(listener);
    }


    public ActionIcon getAddAction() { return addAction; }

    public ActionIcon getRemoveAction() { return removeAction; }


    public void refresh(List<T> items) {
        _listBox.setItems(items);
        _listBox.getListDataView().refreshAll();
        heading.setText(buildLabelText(items));
        addAction.reset();
        removeAction.reset();
    }


    protected Label createTitle(List<T> items) {
        heading = new Prompt(buildLabelText(items));
        return heading;
    }


    protected ListBox<T> createList(List<T> items, ItemLabelGenerator<T> labelGenerator) {
        _listBox.setItems(items);
        _listBox.setItemLabelGenerator(labelGenerator);
        _listBox.setWidth("210px");
        _listBox.getElement().getStyle().set("overflow-y", "scroll");
        _listBox.getElement().getStyle().set("flex-grow", "1");
        addDivider(_listBox);
        return _listBox;
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

    
    private String buildLabelText(List<T> items) {
        return String.format("%s (%d)", _label, items.size());
    }


    private void addDivider(Component component) {
        component.getElement().getStyle().set("border-left", "3px solid lightgray");
    }

}
