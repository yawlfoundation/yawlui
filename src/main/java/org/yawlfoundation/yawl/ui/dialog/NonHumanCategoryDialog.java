package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.lang3.StringUtils;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanCategory;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanResource;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanSubCategory;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.ResourceList;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 28/9/2022
 */
public class NonHumanCategoryDialog extends AbstractDialog {

    private final TextField _nameField = new TextField("Name");
    private final TextArea _descField = new TextArea("Description");
    private final Button _okButton = new Button("OK");
    private final ComboBox<NonHumanResource> _memberCombo = new ComboBox<>("Members");

    private final List<NonHumanSubCategory> _subCategoryListItems = new ArrayList<>();

    private final ResourceClient _resClient;
    private final List<NonHumanCategory> _allCategories;
    private final List<NonHumanResource> _members;
    private final NonHumanCategory _category;


    public NonHumanCategoryDialog(ResourceClient resClient,
                                  List<NonHumanCategory> allCategories,
                                  List<NonHumanResource> members,
                                  NonHumanCategory category) {
        super((category != null ? "Edit" : "Add") + " Category");
        _resClient = resClient;
        _allCategories = allCategories;
        _members = members;
        _category = category;
        addComponent(createForm());
        if (isEditing()) {
            populateForm();
        }
        createButtons(_okButton);
        setWidth("700px");
    }


    public Button getOkButton() { return _okButton; }

    public boolean validate() { return validateName(); }


    public void updateService() {
        String name = _nameField.getValue();
        String description = _descField.getValue();
        if (description.isEmpty()) description = null;

        if (isEditing()) {
            if (hasUpdates(name, description)) {
                _category.setName(name);
                _category.setDescription(description);

                update(_category);
            }
        }
        else {

            // temp id will be replaced with actual id when added to service
            NonHumanCategory category = new NonHumanCategory(name);
            category.setID("NHC-TEMP");
            category.setDescription(description);
            _subCategoryListItems.forEach(item -> {
                if (! item.getName().equals("None")) {
                    category.addSubCategory(item);
                }
            });
            add(category);
        }
    }


    private FormLayout createForm() {
        FormLayout leftSide = new FormLayout();
        leftSide.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        leftSide.add(_nameField, 1);
        leftSide.add(_memberCombo, 1);
        leftSide.add(_descField, 2);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.add(leftSide, 2);
        form.add(createSubCategoryList(), 1);

        _nameField.focus();
        _nameField.setRequired(true);
        _nameField.setAutocomplete(Autocomplete.OFF);
        _descField.setAutocomplete(Autocomplete.OFF);
        _descField.setHeight("100px");
        
        if (! isEditing()) {
            _memberCombo.setVisible(false);
        }
        return form;
    }


    private void populateForm() {
        _nameField.setValue(_category.getName());
        String description = _category.getDescription();
        if (description != null) {
            _descField.setValue(description);
        }
        initMembersCombo();
    }


    private boolean validateName() {
        _nameField.setInvalid(false);
        if (_nameField.isEmpty()) {
            _nameField.setErrorMessage("A name is required");
            _nameField.setInvalid(true);
        }
        else if (! isUniqueName(_nameField.getValue())) {
            _nameField.setErrorMessage("That category name is already registered");
            _nameField.setInvalid(true);
        }
        return !_nameField.isInvalid();
    }


    private boolean isUniqueName(String name) {
        for (NonHumanCategory item : _allCategories) {
            if (name.equals(item.getName())) {
                if (isEditing() && item.equals(_category)) {
                    continue;                             // editing & no name change
                }
                return false;
            }
        }
        return true;
    }


    private boolean isEditing() {
        return _category != null;
    }


    protected void initMembersCombo() {
        _members.sort(Comparator.comparing(NonHumanResource::getName));
        _memberCombo.setItems(_members);
        _memberCombo.setItemLabelGenerator(NonHumanResource::getName);
        String placeHolder = _members.size() + " resource" + (_members.size() > 1 ? "s" : "");
        _memberCombo.setPlaceholder(placeHolder);
        _memberCombo.addValueChangeListener(e -> {
            _memberCombo.setValue(null);
            _memberCombo.setPlaceholder(placeHolder);
        });
    }


    protected ResourceList<NonHumanSubCategory> createSubCategoryList() {
        if (isEditing()) {
            _subCategoryListItems.addAll(_category.getSubCategories());
        }
        else {
            NonHumanSubCategory subCatNone = new NonHumanSubCategory("None");
            _subCategoryListItems.add(subCatNone);
        }
        sortSubCategories();
        ResourceList<NonHumanSubCategory> subCategoryList =
                new ResourceList<>("Sub-Categories", _subCategoryListItems,
                NonHumanSubCategory::getName);
        subCategoryList.addListSelectionListener(event -> {
            NonHumanSubCategory selected = event.getValue();
            if (selected != null) {
                subCategoryList.getRemoveAction().setEnabled(
                        ! selected.getName().equals("None"));
            }
        });
        subCategoryList.addRemoveButtonListener(event -> {
            if (! subCategoryList.getSelected().getName().equals("None")) {
                _subCategoryListItems.remove(subCategoryList.getSelected());
                subCategoryList.refresh(_subCategoryListItems);
            }
        });
        subCategoryList.addAddButtonListener(event -> {
            SingleValueDialog dialog = new SingleValueDialog("Add Sub-category",
                    "Please provide a name for the new sub-category");
            dialog.setPrompt("Name");
            dialog.getOKButton().addClickListener(c -> {
                String name = dialog.getValue();
                dialog.close();
                if (! name.isEmpty() && isUniqueSubCategory(_subCategoryListItems, name)) {
                    NonHumanSubCategory newSubCat = new NonHumanSubCategory(name);
                    _subCategoryListItems.add(newSubCat);
                    sortSubCategories();
                    subCategoryList.refresh(_subCategoryListItems);
                    subCategoryList.select(newSubCat);
                }
            });
            dialog.open();
        });
        subCategoryList.select(_subCategoryListItems.get(0));
        subCategoryList.setHeight("180px");
        return subCategoryList;
    }


    private boolean isUniqueSubCategory(List<NonHumanSubCategory> items, String name) {
        for (NonHumanSubCategory item : items) {
            if (name.equals(item.getName())) {
                return false;
            }
        }
        return true;
    }


    private void sortSubCategories() {
        _subCategoryListItems.sort(Comparator.comparing(NonHumanSubCategory::getName));
    }
    
    private boolean hasUpdates(String name, String description) {
        return ! (name.equals(_category.getName()) &&
                StringUtils.equals(_category.getDescription(), description) &&
                _category.getSubCategories().containsAll(_subCategoryListItems) &&
                new HashSet<>(_subCategoryListItems).containsAll(_category.getSubCategories()));
    }


    private void add(NonHumanCategory category) {
         try {
             _resClient.addNonHumanCategory(category);
             Announcement.success("Non-human category '%s' added", category.getName());
         }
         catch (IOException | ResourceGatewayException ioe) {
             Announcement.warn("Failed to add non-human category '%s': %s",
                     category.getName(), ioe.getMessage());
         }
     }


    private void update(NonHumanCategory category) {
        try {
            _resClient.updateNonHumanCategory(category);
            updateSubCategories(category.getSubCategories(), _subCategoryListItems,
                    _category.getID());
            Announcement.success("Non-human category '%s' updated",
                    category.getName());
        }
        catch (IOException | ResourceGatewayException ioe) {
            Announcement.warn("Failed to update non-human category '%s': %s",
                    category.getName(), ioe.getMessage());
        }
    }


    private void updateSubCategories(Collection<NonHumanSubCategory> oldList,
                                       Collection<NonHumanSubCategory> newList,
                                       String catID) {
        removeSubCategories(notInOther(oldList, newList), catID);
        addSubCategories(notInOther(newList, oldList), catID);
    }


    private List<NonHumanSubCategory> notInOther(Collection<NonHumanSubCategory> master,
                                           Collection<NonHumanSubCategory> other) {
        return master.stream().filter(
                        m -> other.stream().noneMatch(o -> m.getID() == o.getID()))
                .collect(Collectors.toList());
    }


    private void addSubCategories(List<NonHumanSubCategory> toAdd, String catID) {
        toAdd.forEach(s -> {
            try {
                _resClient.addNonHumanSubCategory(catID, s.getName());
            }
            catch (IOException | ResourceGatewayException e) {
                Announcement.warn("Failed to add sub-category '%s': %s",
                        s.getName(), e.getMessage());
            }
        });
    }


    private void removeSubCategories(List<NonHumanSubCategory> toRemove, String catID) {
         toRemove.forEach(s -> {
             try {
                 _resClient.removeNonHumanSubCategory(catID, s.getName());
             }
             catch (IOException | ResourceGatewayException e) {
                 Announcement.warn("Failed to remove sub-category '%s': %s",
                         s.getName(), e.getMessage());
             }
         });
     }

}
