package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.component.Prompt;
import org.yawlfoundation.yawl.ui.component.ResourceMemberList;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Michael Adams
 * @date 08/09/22
 */
public abstract class AbstractOrgDataDialog<T extends AbstractResourceAttribute>
        extends AbstractDialog {

    private final TextField _nameField = new TextField("Name");
    private final TextArea _descriptionField = new TextArea("Description");
    private final TextArea _notesField = new TextArea("Notes");

    private final List<T> _existingItems;
    private final T _item;
    private final List<Participant> _ogMembers;
    private final List<Participant> _allParticipants;
    private final String _title;

    private List<Participant> _updatedMembers;
    private HorizontalLayout _layout;
    private ResourceMemberList _memberList;
    private Button _saveButton;
    

    public AbstractOrgDataDialog(List<T> items, T item, List<Participant> pList,
                                 List<Participant> members, String title) {
        items.sort(Comparator.comparing(AbstractResourceAttribute::getName));
        _existingItems = items;
        _item = item;
        _allParticipants = pList;
        _ogMembers = members;
        _updatedMembers = members;
        _title = title;
   }


    public void build() {
        setHeader(getTitle());
        addComponent(createForm());
        if (isEditing()) {
            populateForm();
        }
        createButtons();
    }


    abstract void addBelongsToCombo(FormLayout form, T item);

    abstract void addGroupCombo(FormLayout form, T item);

    public abstract T compose();


    public String getMemberHeight() { return "225px"; }

    public String getSelectMembersHeight() { return "192px"; }


    public boolean validate() {
        return validateName();
    }

    public Button getSaveButton() { return _saveButton; }

    protected boolean isEditing() { return _item != null; }

    protected List<T> getExistingItems() { return _existingItems; }
    
    protected T getItem() { return _item; }

    protected String getNameValue() { return _nameField.getValue(); }

    protected String getDescriptionValue() { return _descriptionField.getValue(); }

    protected String getNotesValue() { return _notesField.getValue(); }

    public List<Participant> getStartingMembers() { return _ogMembers; }

    public List<Participant> getUpdatedMembers() { return _updatedMembers; }

    public boolean hasUpdatedMembers() { return ! _updatedMembers.isEmpty(); }



    private String getTitle() {
        return (isEditing() ? "Edit " : "Add ") + _title;
    }

    
    private Component createForm() {
        _nameField.setRequired(true);

        _nameField.setAutocomplete(Autocomplete.OFF);
        _descriptionField.setAutocomplete(Autocomplete.OFF);
        _notesField.setAutocomplete(Autocomplete.OFF);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_nameField, 1);
        addBelongsToCombo(form, _item);
        form.add(_descriptionField, 2);
        form.add(_notesField, 2);
        addGroupCombo(form, _item);

        if (_ogMembers != null) {
            _memberList = new ResourceMemberList(_ogMembers);
            _memberList.setHeight(getMemberHeight());
            _memberList.addAddButtonListener(e -> showSelectNewMembersList());
            _memberList.addRemovebuttonListener(e -> removeSelectedMember());
            _layout = new HorizontalLayout(form, _memberList);
            _layout.setFlexGrow(1, form);
            return _layout;
        }
        return form;
    }


    protected void populateForm() {
        if (_item != null) {
            _nameField.setValue(_item.getName());
            _descriptionField.setValue(_item.getDescription());
            _notesField.setValue(_item.getNotes());
        }
    }


    private void createButtons() {
        Button cancel = new Button("Cancel", event -> close());
        _saveButton = new Button("Save");                 // listener added later
        _saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(cancel, _saveButton);
        getButtonBar().getStyle().set("padding-top", "15px");
    }


    private void showSelectNewMembersList() {
        VerticalLayout container = new UnpaddedVerticalLayout();
        container.setSpacing(false);

        MultiSelectParticipantList listPanel = new MultiSelectParticipantList(
                _allParticipants, null);
        listPanel.getElement().getStyle().set("padding", "0 5px 0 5px");
        listPanel.getElement().getStyle().set("border-left", "3px solid lightgray");
        listPanel.getListbox().setRenderer(new ComponentRenderer<>(p -> {
            Span span = new Span(p.getFullName());
            span.getStyle().set("padding-bottom", "0");
            return span;
        }));
        listPanel.setWidthFull();
        listPanel.setHeight(getSelectMembersHeight());
        listPanel.smaller();
        listPanel.setSelected(_updatedMembers);
        listPanel.addCancelListener(e -> _layout.remove(container));
        listPanel.addOKListener(e -> {
            _layout.remove(container);
            _updatedMembers = new ArrayList<>(listPanel.getSelection());
            _memberList.refresh(_updatedMembers);
        });

        container.add(new Prompt("Select Members"), listPanel);
        _layout.add(container);
    }


    private void removeSelectedMember() {
        Participant p = _memberList.getSelected();
        if (p != null) {
            _updatedMembers.remove(p);
            _memberList.refresh(_updatedMembers);
        }
    }


    private boolean validateName() {
        _nameField.setInvalid(false);
        if (_nameField.isEmpty()) {
            _nameField.setErrorMessage("A name is required");
            _nameField.setInvalid(true);
        }
        else if (! isUniqueName(_nameField.getValue())) {
            _nameField.setErrorMessage("That " + _title + " name is already registered");
            _nameField.setInvalid(true);
        }
        return !_nameField.isInvalid();
    }
    

    private boolean isUniqueName(String name) {
        for (T item : _existingItems) {
            if (name.equals(item.getName())) {
                if (isEditing() && item.equals(_item)) {
                    continue;                             // editing & no name change
                }
                return false;
            }
        }
        return true;
    }

    protected void initCombo(ComboBox<T> combo, List<T> items, T value, T nil) {
        items.add(0, nil);
        combo.setItems(items);
        combo.setItemLabelGenerator(T::getName);
        if (isEditing() && value != null) {
            combo.setValue(value);
        }
        else {
            combo.setValue(nil);
        }
    }


    protected boolean validateCombo(ComboBox<T> combo, String object, String verb) {
        combo.setInvalid(false);
         if (getNameValue().equals(combo.getValue().getName())) {
             combo.setErrorMessage(String.format("A %s cannot %s to itself", object, verb));
             combo.setInvalid(true);
         }
         return ! combo.isInvalid();
     }

}
