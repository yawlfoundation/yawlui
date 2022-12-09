package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.MultiSelectResourceList;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.service.Clients;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class ParticipantDetailsDialog extends AbstractDialog {

    private enum Attribute { Role, Capability, Position }
    private enum Mode { Add, Edit, View }

    private final TextField _userField = new TextField("User ID");
    private final Checkbox _adminCbx = new Checkbox("Administrator");
    private final TextField _firstNameField = new TextField("First Name");
    private final TextField _lastNameField = new TextField("Last Name");
    private final PasswordField _passwordField = new PasswordField("Password");
    private final PasswordField _pwConfirmField = new PasswordField("Confirm Password");
    private final TextArea _notesField = new TextArea("Notes");

    // Privileges
    private final Checkbox _privCases = new Checkbox("Manage cases");
    private final Checkbox _privChoose = new Checkbox("Choose work item to start");
    private final Checkbox _privReorder = new Checkbox("Reorder work items");
    private final Checkbox _privConcurrent = new Checkbox("Start work items concurrently");
    private final Checkbox _privChain = new Checkbox("Chain work item execution");
    private final Checkbox _privViewTeam = new Checkbox("View team's work items");
    private final Checkbox _privViewGroup = new Checkbox("View org group's work items");

    private final MultiSelectResourceList _roleList;
    private final MultiSelectResourceList _capabilityList;
    private final MultiSelectResourceList _positionList;

    private final ResourceClient _resClient = Clients.getResourceClient();
    private final List<Participant> _allParticipants;
    private final Participant _participant;
    private final Mode _mode;

    private Button _okButton;


    // when adding
    public ParticipantDetailsDialog(List<Participant> pList) {
        this(pList, null);
    }


    // when read-only
    public ParticipantDetailsDialog(Participant p) {
         this(null, p);
     }


    // when editing
    public ParticipantDetailsDialog(List<Participant> pList, Participant p) {
        _allParticipants = pList;
        _participant = p;
        _mode = deriveMode(pList, p);
        _roleList = createAttributeList(Attribute.Role);
        _capabilityList = createAttributeList(Attribute.Capability);
        _positionList = createAttributeList(Attribute.Position);
        setHeader(_mode.name() + " Participant");
        addComponent(createContent());
        addComponent(createGroupPanels());
        if (! adding()) {
            populateForm();
        }
        createButtons();
        setWidth("700px");
    }


    public Button getOKButton() { return _okButton; }


    public boolean validate() {

        // no short circuits
        return validateUserID() & validatePassword() & validateNames();
    }


    public void updateService() {
        switch (_mode) {
            case Add : addParticipant(composeParticipant()); break;
            case Edit : updateParticipant(); break;
        }
    }


    private Mode deriveMode(List<Participant> pList, Participant p) {
        if (p == null) return Mode.Add;
        if (pList == null) return Mode.View;
        return Mode.Edit;
    }


    private Participant composeParticipant() {
        String userID = _userField.getValue();
        boolean isAdmin = _adminCbx.getValue();
        String firstName = _firstNameField.getValue();
        String lastName = _lastNameField.getValue();
        String password = _passwordField.getValue();
        String notes = _notesField.getValue();
        if (editing()) {
            _participant.setUserID(userID);
            _participant.setAdministrator(isAdmin);
            _participant.setFirstName(firstName);
            _participant.setLastName(lastName);
            if (! password.isEmpty()) {
                _participant.setPassword(password);
            }
            _participant.setNotes(notes);
            return _participant;
        }
        else if (adding()) {
            Participant p = new Participant(lastName, firstName, userID);
            p.setPassword(password);
            p.setAdministrator(isAdmin);
            p.setNotes(notes);
            updateUserPrivileges(p.getUserPrivileges());
            return p;
        }
        return null;
    }


    private HorizontalLayout createContent() {
        return new HorizontalLayout(createForm(), createPrivilegesPanel());
    }

    private FormLayout createForm() {
        configureFields();
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_userField, 1);
        form.add(_adminCbx, 1);
        form.add(_firstNameField, 1);
        form.add(_lastNameField, 1);
        form.add(_passwordField, 1);
        form.add(_pwConfirmField, 1);
        form.add(_notesField, 2);
        return form;
    }


    private void configureFields() {
        _userField.setAutocomplete(Autocomplete.OFF);
        _firstNameField.setAutocomplete(Autocomplete.OFF);
        _lastNameField.setAutocomplete(Autocomplete.OFF);
        _passwordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
        _pwConfirmField.setAutocomplete(Autocomplete.NEW_PASSWORD);
        _notesField.setAutocomplete(Autocomplete.OFF);

        if (viewing()) {
            _userField.setReadOnly(true);
           _firstNameField.setReadOnly(true);
           _lastNameField.setReadOnly(true);
           _passwordField.setReadOnly(true);
           _pwConfirmField.setReadOnly(true);
           _notesField.setReadOnly(true);
           _adminCbx.setReadOnly(true);
        }
        else {
            _userField.setRequired(true);
            _firstNameField.setRequired(true);
            _lastNameField.setRequired(true);
            if (adding()) {
                _passwordField.setRequired(true);
                _pwConfirmField.setRequired(true);
            }
        }
    }


    private VerticalLayout createPrivilegesPanel() {
        if (! adding()) {
            UserPrivileges up = getUserPrivileges();
            _privCases.setValue(up.canManageCases());
            _privChoose.setValue((up.canChooseItemToStart()));
            _privReorder.setValue(up.canReorder());
            _privConcurrent.setValue(up.canStartConcurrent());
            _privChain.setValue(up.canChainExecution());
            _privViewTeam.setValue(up.canViewTeamItems());
            _privViewGroup.setValue(up.canViewOrgGroupItems());
            _participant.setUserPrivileges(up);
            if (viewing()) {
                _privCases.setReadOnly(true);
               _privChoose.setReadOnly(true);
               _privReorder.setReadOnly(true);
               _privConcurrent.setReadOnly(true);
               _privChain.setReadOnly(true);
               _privViewTeam.setReadOnly(true);
               _privViewGroup.setReadOnly(true);
            }
        }
        return new UnpaddedVerticalLayout("t", new H5("Privileges"), _privCases,
                _privChoose, _privReorder, _privConcurrent, _privChain, _privViewTeam,
                _privViewGroup);
    }


    private HorizontalLayout createGroupPanels() {
        return new HorizontalLayout(
                createGroupPanel(_roleList, "Roles"),
                createGroupPanel(_capabilityList, "Capabilities"),
                createGroupPanel(_positionList, "Positions")
        );
    }


    private MultiSelectResourceList createAttributeList(Attribute attribute) {
        MultiSelectResourceList list = new MultiSelectResourceList(getAttributeList(attribute));
        
        if (! adding()) {
            list.select(getParticipantAttributes(attribute));
        }
        return list;
    }


    private VerticalLayout createGroupPanel(MultiSelectResourceList list, String title) {
        VerticalLayout layout = new UnpaddedVerticalLayout();
        layout.setHeight("220px");
        H5 header = new H5(String.format("%s (%d)", title, list.getSelectedItems().size()));
        layout.add(header);
        layout.add(list);
        list.setReadOnly(viewing());

        list.addValueChangeListener(e -> header.getElement().setText(
                String.format("%s (%d)", title, list.getSelectedItems().size())));
 
        return layout;
    }

    
    private void createButtons() {
        getButtonBar().getStyle().set("margin-top", "10px");
        if (! viewing()) {
            getButtonBar().add(new Button("Cancel", event -> close()));
            _okButton = new Button("OK");                 // listener added later
        }
        else {
            _okButton = new Button("Close", e -> close());
        }
        _okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(_okButton);
    }


    private void populateForm() {
        if (_participant != null) {
            _userField.setValue(_participant.getUserID());
            _adminCbx.setValue(_participant.isAdministrator());
            _firstNameField.setValue(_participant.getFirstName());
            _lastNameField.setValue(_participant.getLastName());
            if (_participant.getNotes() != null) {
                _notesField.setValue(_participant.getNotes());
            }
        }
    }


    private boolean validateUserID() {
        _userField.setInvalid(false);
        if (_userField.isEmpty()) {
            _userField.setErrorMessage("A user id is required");
            _userField.setInvalid(true);
        }
        else if (! isUniqueUserID(_userField.getValue())) {
            _userField.setErrorMessage("That user id is already registered - try again");
            _userField.setInvalid(true);
        }
        return ! _userField.isInvalid();
    }


    private boolean validateNames() {
        _firstNameField.setInvalid(false);
        if (_firstNameField.isEmpty()) {
            _firstNameField.setErrorMessage("A first name is required");
            _firstNameField.setInvalid(true);
        }
        _lastNameField.setInvalid(false);
        if (_lastNameField.isEmpty()) {
            _lastNameField.setErrorMessage("A last name is required");
            _lastNameField.setInvalid(true);
        }
        return ! (_firstNameField.isInvalid() || _lastNameField.isInvalid());
    }


    private boolean validatePassword() {
        _passwordField.setInvalid(false);
        _pwConfirmField.setInvalid(false);
        if (editing() && _passwordField.isEmpty() && _pwConfirmField.isEmpty()) {
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


    private boolean isUniqueUserID(String id) {

        // if editing an existing user, and there's no change to the id, don't check further
        if (editing() && id.equals(_participant.getUserID())) {
            return true;
        }

        // otherwise, it's a new user or existing user with a change
        if (id.equalsIgnoreCase("admin")) {
            return false;                                        // 'admin' is reserved
        }
        for (Participant p : _allParticipants) {
            if (id.equals(p.getUserID())) {
                return false;
            }
        }
        return true;
    }


    private boolean hasUserPrivilegeChanges() {
        if (adding()) {
            return true;
        }
        UserPrivileges up = _participant.getUserPrivileges();
        return ! (
                _privCases.getValue() == up.canManageCases() &&
                _privChoose.getValue() == up.canChooseItemToStart() &&
                _privReorder.getValue() == up.canReorder() &&
                _privConcurrent.getValue() == up.canStartConcurrent() &&
                _privChain.getValue() == up.canChainExecution() &&
                _privViewTeam.getValue() == up.canViewTeamItems() &&
                _privViewGroup.getValue() == up.canViewOrgGroupItems()
                );
    }


    private void updateUserPrivileges(UserPrivileges up) {
        up.setCanManageCases(_privCases.getValue());
        up.setCanChooseItemToStart(_privChoose.getValue());
        up.setCanReorder(_privReorder.getValue());
        up.setCanStartConcurrent(_privConcurrent.getValue());
        up.setCanChainExecution(_privChain.getValue());
        up.setCanViewTeamItems(_privViewTeam.getValue());
        up.setCanViewOrgGroupItems(_privViewGroup.getValue());
    }


    private List<AbstractResourceAttribute> getAttributeList(Attribute attribute) {
        try {
            switch (attribute) {
                case Role : return _resClient.getRoles();
                case Capability : return _resClient.getCapabilities();
                case Position : return _resClient.getPositions();
            }
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                    "Failed to retrieve %s list from engine : %s",
                    attribute.name(), e.getMessage());
        }
        return Collections.emptyList();
    }


    private List<AbstractResourceAttribute> getParticipantAttributes(Attribute attribute) {
        switch (attribute) {
            case Role : return new ArrayList<>(_participant.getRoles());
            case Capability : return new ArrayList<>(_participant.getCapabilities());
            case Position : return new ArrayList<>(_participant.getPositions());
        }
        return Collections.emptyList();
    }


    private UserPrivileges getUserPrivileges() {
        try {
            return _resClient.getUserPrivileges(_participant.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                    "Failed to retrieve user privileges from engine for %s: %s",
                    _participant.getFullName(), e.getMessage());
         }
        return _participant.getUserPrivileges();
    }


    private void addParticipant(Participant p) {
        try {
            String pid = _resClient.addParticipant(p);
            if (_resClient.successful(pid)) {
                p.setID(pid);
                _resClient.setUserPrivileges(p);
                for (AbstractResourceAttribute r : _roleList.getSelectedItems()) {
                    _resClient.addParticipantToRole(p.getID(), r.getID());
                }
                for (AbstractResourceAttribute c : _capabilityList.getSelectedItems()) {
                    _resClient.addParticipantToCapability(p.getID(), c.getID());
                }
                for (AbstractResourceAttribute pos : _positionList.getSelectedItems()) {
                    _resClient.addParticipantToPosition(p.getID(), pos.getID());
                }
                Announcement.success("Added participant '%s'", p.getFullName());
            }
            else {
                Announcement.warn(StringUtil.unwrap(pid));  // an error msg
            }
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private void updateParticipant() {
        try {
            if (hasPropertyChanges()) {
                _resClient.updateParticipant(composeParticipant());
            }
            if (hasUserPrivilegeChanges()) {
                updateUserPrivileges(_participant.getUserPrivileges());
                _resClient.setUserPrivileges(_participant);
            }
            String pid = _participant.getID();
            for (AbstractResourceAttribute r : getNotInOther(
                    _roleList.getSelectedItems(), _participant.getRoles())) {
                _resClient.addParticipantToRole(pid, r.getID());
            }
            for (AbstractResourceAttribute c : getNotInOther(
                    _capabilityList.getSelectedItems(), _participant.getCapabilities())) {
                _resClient.addParticipantToCapability(pid, c.getID());
            }
            for (AbstractResourceAttribute pos : getNotInOther(
                    _positionList.getSelectedItems(), _participant.getPositions())) {
                _resClient.addParticipantToPosition(pid, pos.getID());
            }
            for (AbstractResourceAttribute r : getNotInOther(
                    _participant.getRoles(), _roleList.getSelectedItems())) {
                 _resClient.removeParticipantFromRole(pid, r.getID());
             }
            for (AbstractResourceAttribute c : getNotInOther(
                    _participant.getCapabilities(), _capabilityList.getSelectedItems())) {
                 _resClient.removeParticipantFromCapability(pid, c.getID());
             }
            for (AbstractResourceAttribute pos : getNotInOther(
                    _participant.getPositions(), _positionList.getSelectedItems())) {
                 _resClient.removeParticipantFromPosition(pid, pos.getID());
             }
             Announcement.success("Updated participant '%s'",
                     _participant.getFullName());
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
        }
    }


    private boolean hasPropertyChanges() {
        return ! (_passwordField.getValue().isEmpty() &&
                _userField.getValue().equals(_participant.getUserID()) &&
                _participant.isAdministrator() == _adminCbx.getValue() &&
                _firstNameField.getValue().equals(_participant.getFirstName()) &&
                _lastNameField.getValue().equals(_participant.getLastName()) &&
                _notesField.getValue().equals(_participant.getNotes()));
    }


    private List<AbstractResourceAttribute> getNotInOther(
            Set<? extends AbstractResourceAttribute> master,
            Set<? extends AbstractResourceAttribute> other) {

        return master.stream().filter(
                        m -> other.stream().noneMatch(
                                o -> m.getID().equals(o.getID())))
                .collect(Collectors.toList());
    }


    private boolean adding() { return _mode == Mode.Add; }

    private boolean viewing() { return _mode == Mode.View; }

    private boolean editing() { return _mode == Mode.Edit; }

}
