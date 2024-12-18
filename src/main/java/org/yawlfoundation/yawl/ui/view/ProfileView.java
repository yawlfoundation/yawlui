package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.SingleSelectResourceList;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;

import java.io.IOException;
import java.util.*;


/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class ProfileView extends AbstractView {

    private enum Attribute { Role, Capability, Position }

    private final TextField _userField = new TextField("User ID");
    private final Checkbox _adminCbx = new Checkbox("Administrator");
    private final TextField _firstNameField = new TextField("First Name");
    private final TextField _lastNameField = new TextField("Last Name");
    private final EmailField _emailField = new EmailField("Email");
    private final CheckboxGroup<String> _emailCheckboxGroup = new CheckboxGroup<>("Email notifications");
    private final PasswordField _passwordField = new PasswordField("Password");
    private final PasswordField _pwConfirmField = new PasswordField("Confirm Password");

    private final SingleSelectResourceList _roleList;
    private final SingleSelectResourceList _capabilityList;
    private final SingleSelectResourceList _positionList;

    private static final String OFFER_LABEL = "On Offer";
    private static final String ALLOCATION_LABEL = "On Allocation";

    private final Participant _participant;
    

    public ProfileView(Participant participant) {
        super();
        _participant = participant;
        _roleList = createAttributeList(Attribute.Role);
        _capabilityList = createAttributeList(Attribute.Capability);
        _positionList = createAttributeList(Attribute.Position);
        add(createLayout());
        populateForm();
        setWidth("900px");
    }


    @Override
    Component createLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.add(createHeader("My Profile"));
        layout.add(createForm());
        return layout;
    }


    private FormLayout createForm() {
        configureFields();
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_userField, 1);
        form.add(_adminCbx, 1);
        form.add(_firstNameField, 1);
        form.add(_emailField, 1);
        form.add(_lastNameField, 1);
    
        _emailCheckboxGroup.setItems(OFFER_LABEL, ALLOCATION_LABEL);
        form.add(_emailCheckboxGroup, 1);
        
        form.add(createGroupPanels(), 2);
        form.add(createPasswordForm(), 2);
        form.setWidthFull();
        return form;
    }


    private void configureFields() {
        _passwordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
        _pwConfirmField.setAutocomplete(Autocomplete.NEW_PASSWORD);

        _userField.setReadOnly(true);
        _firstNameField.setReadOnly(true);
        _lastNameField.setReadOnly(true);
        _adminCbx.setReadOnly(true);
        _emailField.setReadOnly(true);
    }

    
    private HorizontalLayout createGroupPanels() {
        VerticalLayout roleLayout = createGroupPanel(_roleList, "Roles");
        VerticalLayout capabilityLayout = createGroupPanel(_capabilityList, "Capabilities");
        VerticalLayout positionLayout = createGroupPanel(_positionList, "Positions");
        HorizontalLayout layout = new HorizontalLayout(
                roleLayout, capabilityLayout, positionLayout);
        layout.setWidthFull();
        return layout;
    }


    private VerticalLayout createPasswordForm() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.CLOSE, ActionIcon.RED, "Cancel", e -> {
            _passwordField.clear();
            _pwConfirmField.clear();
            validatePassword();                              // clears error messages
        });
        ribbon.add(VaadinIcon.CHECK, ActionIcon.GREEN, "OK", e -> {
            if (!_passwordField.isEmpty() && validatePassword()) {
                _participant.setPassword(_passwordField.getValue());
                updateParticipant("Password");
            }
        });
        ribbon.setJustifyContentMode(JustifyContentMode.END);
        ribbon.getStyle().set("margin", "10px 0 5px 0");
        ribbon.getStyle().set("padding-right", "5px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_passwordField, 1);
        form.add(_pwConfirmField, 1);
        form.add(ribbon, 2);

        VerticalLayout layout = new VerticalLayout(form);  // inside VL for correct spacing
        layout.getStyle().set("border", "1px solid lightgray");
        layout.getStyle().set("margin-top", "10px");
        layout.getStyle().set("padding-bottom", "0");
        return layout;
    }

    
    protected boolean validatePassword() {
        _passwordField.setInvalid(false);
        _pwConfirmField.setInvalid(false);
        if (_passwordField.isEmpty() && _pwConfirmField.isEmpty()) {
            return true;
        }
        if (_passwordField.isEmpty()) {
            _passwordField.setErrorMessage("A password is required");
            _passwordField.setInvalid(true);
        }
        if (_pwConfirmField.isEmpty()) {
            _pwConfirmField.setErrorMessage("A confirm password is required");
            _pwConfirmField.setInvalid(true);
        }
        if (! (_passwordField.isInvalid() || _pwConfirmField.isInvalid() ||
                _pwConfirmField.getValue().equals(_passwordField.getValue()))) {
            _pwConfirmField.setErrorMessage("Password fields don't match");
            _pwConfirmField.setInvalid(true);
        }
        return ! (_passwordField.isInvalid() || _pwConfirmField.isInvalid());
    }


    private SingleSelectResourceList createAttributeList(Attribute attribute) {
        SingleSelectResourceList resourceList =
                new SingleSelectResourceList(getAttributes(attribute));
        resourceList.setWidth("100%");
        return resourceList;
    }


    private VerticalLayout createGroupPanel(SingleSelectResourceList list, String title) {
        VerticalLayout layout = new UnpaddedVerticalLayout();
        layout.setHeight("220px");
        layout.setWidth("280px");
        H5 header = new H5(String.format("%s (%d)", title, list.getChildren().count()));
        layout.add(header);
        layout.add(list);
        list.setReadOnly(true);
        return layout;
    }


    private void populateForm() {
        if (_participant != null) {
            _userField.setValue(_participant.getUserID());
            _adminCbx.setValue(_participant.isAdministrator());
            _firstNameField.setValue(_participant.getFirstName());
            _lastNameField.setValue(_participant.getLastName());
            _emailField.setValue(_participant.getEmail());

            Set<String> selections = new HashSet<>();
            if (_participant.isEmailOnOffer()) selections.add(OFFER_LABEL);
            if (_participant.isEmailOnAllocation()) selections.add(ALLOCATION_LABEL);
            _emailCheckboxGroup.select(selections);
            _emailCheckboxGroup.addSelectionListener(listener -> {
                Set<String> selected = listener.getAllSelectedItems();
                _participant.setEmailOnOffer(selected.contains(OFFER_LABEL));
                _participant.setEmailOnAllocation(selected.contains(ALLOCATION_LABEL));
                updateParticipant("Notification choice");
            });
        }
    }


    private List<AbstractResourceAttribute> getAttributes(Attribute attribute) {
        switch (attribute) {
            case Role : return new ArrayList<>(_participant.getRoles());
            case Capability : return new ArrayList<>(_participant.getCapabilities());
            case Position : return new ArrayList<>(_participant.getPositions());
        }
        return Collections.emptyList();
    }


    private void updateParticipant(String item) {
        try {
            getResourceClient().updateParticipant(_participant);
            Announcement.success(item + " updated");
        }
        catch (IOException ioe) {
            Announcement.error(item + "failed to update: " + ioe.getMessage());
        }
    }

}
