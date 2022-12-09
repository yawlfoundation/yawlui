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
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.Clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 28/9/2022
 */
public class NonHumanResourceDialog extends AbstractDialog {

    private final TextField _nameField = new TextField("Name");
    private final TextArea _descField = new TextArea("Description");
    private final ComboBox<NonHumanCategory> _categoryCombo =
            new ComboBox<>("Category");
    private final ComboBox<String> _subCategoryCombo = new ComboBox<>("Sub-category");
    private final Button _okButton = new Button("OK");

    private final List<NonHumanResource> _allResources;
    private final NonHumanResource _resource;


    public NonHumanResourceDialog(List<NonHumanResource> allResources,
                                  NonHumanResource resource) {
        super((resource != null ? "Edit" : "Add") + " Resource");
        _allResources = allResources;
        _resource = resource;
        addComponent(createForm());
        if (isEditing()) {
            populateForm();
        }
        createButtons(_okButton);
        setWidth("500px");
    }


    public Button getOkButton() { return _okButton; }

    public boolean validate() { return validateName(); }


    public void updateService() {
        String name = _nameField.getValue();
        String description = _descField.getValue();
        if (description.isEmpty()) description = null;
        NonHumanCategory category = _categoryCombo.getValue();
        if ("None".equals(category.getName())) category = null;
        String subCategory = category == null ? null : _subCategoryCombo.getValue();

        if (isEditing()) {
            if (hasUpdates(name, description, category, subCategory)) {
                _resource.setName(name);
                _resource.setDescription(description);
                _resource.setCategory(category);
                _resource.setSubCategory(subCategory);
                update(_resource);
            }
        }
        else {

            // temp id will be replaced with actual id when added to service
            NonHumanResource resource = new NonHumanResource("NH-TEMP");
            resource.setName(name);
            resource.setDescription(description);
            resource.setCategory(category);
            resource.setSubCategory(subCategory);
            add(resource);
        }
    }


    private FormLayout createForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_nameField, 2);
        form.add(_descField, 2);
        form.add(_categoryCombo, 1);
        form.add(_subCategoryCombo, 1);

        _nameField.focus();
        _nameField.setRequired(true);
        _nameField.setAutocomplete(Autocomplete.OFF);
        _descField.setAutocomplete(Autocomplete.OFF);

        initSubCategoryCombo();
        initCategoryCombo();

        return form;
    }


    private void populateForm() {
        _nameField.setValue(_resource.getName());
        _descField.setValue(_resource.getDescription());

        NonHumanCategory category = _resource.getCategory();
        if (category != null) {
            _categoryCombo.setValue(category);   // triggers subcat change too
        }
    }


    private boolean validateName() {
        _nameField.setInvalid(false);
        if (_nameField.isEmpty()) {
            _nameField.setErrorMessage("A name is required");
            _nameField.setInvalid(true);
        }
        else if (! isUniqueName(_nameField.getValue())) {
            _nameField.setErrorMessage("That resource name is already registered");
            _nameField.setInvalid(true);
        }
        return !_nameField.isInvalid();
    }


    private boolean isUniqueName(String name) {
        for (NonHumanResource item : _allResources) {
            if (name.equals(item.getName())) {
                if (isEditing() && item.equals(_resource)) {
                    continue;                             // editing & no name change
                }
                return false;
            }
        }
        return true;
    }


    private boolean isEditing() {
        return _resource != null;
    }


    protected void initCategoryCombo() {
        List<NonHumanCategory> catList = new ArrayList<>();
        NonHumanCategory nilCat = new NonHumanCategory("None");
        catList.add(nilCat);
        catList.addAll(getAllCategories());
        _categoryCombo.setItems(catList);
        _categoryCombo.setItemLabelGenerator(NonHumanCategory::getName);
        _categoryCombo.setValue(nilCat);
        _categoryCombo.addValueChangeListener(e -> refreshSubCategoryCombo(e.getValue()));
    }


    protected void initSubCategoryCombo() {
        List<String> subCatList = new ArrayList<>();
        if (isEditing() && _resource.getCategory() != null) {
            subCatList.addAll(getSubCategories(_resource.getCategory()));
            _subCategoryCombo.setItems(subCatList);
            String subCatName = _resource.getSubCategoryName();
            if (subCatName != null) {
                _subCategoryCombo.setValue(subCatName);
            }
        }
        else {
            subCatList.add("None");
            _subCategoryCombo.setItems(subCatList);
            _subCategoryCombo.setValue("None");
            _subCategoryCombo.setEnabled(false);       // depends on category selection
        }
    }


    private void refreshSubCategoryCombo(NonHumanCategory category) {
        _subCategoryCombo.setEnabled(! category.getName().equals("None"));
        if (_subCategoryCombo.isEnabled()) {
            _subCategoryCombo.setItems(getSubCategories(category));
        }
        String value = "None";
        if (isEditing() && _resource.getCategoryName().equals(category.getName())) {
                value = _resource.getSubCategoryName();
        }
        _subCategoryCombo.setValue(value);
    }


    private boolean hasUpdates(String name, String description, NonHumanCategory category,
                                   String subCategory) {
        return ! (name.equals(_resource.getName()) &&
                StringUtils.equals(_resource.getDescription(), description) &&
                categoryEquals(_resource.getCategory(), category) &&
                subCategoryEquals(_resource.getSubCategoryName(), subCategory));
    }


    private boolean categoryEquals(NonHumanCategory oldCat, NonHumanCategory newCat) {
        if (oldCat == null) {
            return newCat == null;
        }
        else {
            return newCat != null && newCat.getName().equals(oldCat.getName());
        }
    }


    private boolean subCategoryEquals(String oldSubCat, String newSubCat) {
        if (oldSubCat == null || oldSubCat.equals("None")) {
            return newSubCat == null || newSubCat.equals("None");
        }
        else {
            return newSubCat != null && newSubCat.equals(oldSubCat);
        }
    }


    private void update(NonHumanResource resource) {
        try {
            Clients.getResourceClient().updateNonHumanResource(resource);
            Announcement.success("Non-human resource '%s' updated",
                    resource.getName());
        }
        catch (IOException ioe) {
            Announcement.warn("Failed to update non-human resource '%s': %s",
                    resource.getName(), ioe.getMessage());
        }
    }


    private void add(NonHumanResource resource) {
        try {
            Clients.getResourceClient().addNonHumanResource(resource);
            Announcement.success("Non-human resource '%s' added", resource.getName());
        }
        catch (IOException ioe) {
            Announcement.warn("Failed to add non-human resource '%s': %s",
                    resource.getName(), ioe.getMessage());
        }
    }

    
    private List<NonHumanCategory> getAllCategories() {
        try {
            List<NonHumanCategory> catList = Clients.getResourceClient()
                    .getNonHumanCategories();
            Collections.sort(catList);
            return catList;
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(
                    "Failed to get list of non-human categories from service: " +
                            e.getMessage());
            return Collections.emptyList();
        }
    }


    private List<String> getSubCategories(NonHumanCategory category) {
        try {
            List<String> catList = Clients.getResourceClient()
                    .getNonHumanSubCategories(category.getID());
            Collections.sort(catList);
            return catList;
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(
                    "Failed to get list of non-human sub-categories from service: " +
                            e.getMessage());
        }
        return Collections.emptyList();
    }

}
