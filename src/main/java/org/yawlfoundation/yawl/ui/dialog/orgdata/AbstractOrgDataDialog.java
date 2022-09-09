package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.OrgGroup;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;

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
    private final List<Participant> _pList;
    private final String _title;

    private Button _saveButton;
    private ComboBox<OrgGroup.GroupType> _ogTypeCombo;
    private MultiSelectParticipantList _membersList;



    public AbstractOrgDataDialog(List<T> items, T item, List<Participant> pList, String title) {
        items.sort(Comparator.comparing(AbstractResourceAttribute::getName));
        _existingItems = items;
        _item = item;
        _pList = pList;
        _title = title;
   }


    public void build() {
        setHeader(getTitle());
        addComponent(createForm());
        if (isEditing()) {
            populateForm();
        }
        createButtons();
        setHeight("350px");
    }


    abstract void addBelongsToCombo(FormLayout form, T item);

    abstract void addGroupCombo(FormLayout form, T item);

    abstract MultiSelectParticipantList createMembersList(T item);

    public abstract T compose();
    

    public boolean validate() {
        return validateName() & validateDescription();       // no short circuits
    }

    public Button getSaveButton() { return _saveButton; }


    protected boolean isEditing() { return _item != null; }

    protected List<T> getExistingItems() { return _existingItems; }

    protected List<Participant> getParticipantList() { return _pList; }

    protected T getItem() { return _item; }

    
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

        _membersList = createMembersList(_item);
        if (_membersList != null) {
            _membersList.setPadding(false);
            _membersList.getStyle().set("border", "1px solid lightgray");
            Label label = new Label(buildLabelText());
            label.getStyle().set("color", "var(--lumo-secondary-text-color)");
            label.getStyle().set("font-weight", "500");
            label.getStyle().set("font-size", "var(--lumo-font-size-s)");
            label.getStyle().set("margin-top", "7px");
            label.getStyle().set("margin-bottom", "2px");
            _membersList.getListbox().addValueChangeListener(v ->
                    label.setText(buildLabelText()));
//            HorizontalLayout headLayout = new HorizontalLayout();
//            headLayout.setSpacing(false);
//            Checkbox selectedOnly = new Checkbox("Selected Only",
//                    e -> {
//                if (e.getValue())
//                    }
//            );
            VerticalLayout vLayout = new UnpaddedVerticalLayout("tb",
                    label, _membersList);
            vLayout.setSpacing(false);
            vLayout.setWidth("60%");
            vLayout.setHeight("200px");
            return new HorizontalLayout(form, vLayout);
        }
        return form;
    }


    private String buildLabelText() {
        return String.format("Members (%d)", _membersList.getSelection().size());
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


    private boolean validateDescription() {
        _descriptionField.setInvalid(false);
        if (_descriptionField.isEmpty()) {
            _descriptionField.setErrorMessage("A description is required");
            _descriptionField.setInvalid(true);
        }
        return !_descriptionField.isInvalid();
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

}
