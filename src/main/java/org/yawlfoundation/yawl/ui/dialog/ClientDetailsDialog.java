package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.util.HttpURLValidator;
import org.yawlfoundation.yawl.util.StringUtil;

import java.util.List;

/**
 * @author Michael Adams
 * @date 12/11/20
 */
public class ClientDetailsDialog<T extends YClient> extends AbstractDialog {

    private final TextField _nameField = new TextField("Name");
    private final TextField _uriField = new TextField("URI");
    private final PasswordField _passwordField = new PasswordField("Password");
    private final PasswordField _pwConfirmField = new PasswordField("Confirm Password");
    private final TextArea _descriptionField = new TextArea("Description");
    private final Button _saveButton = new Button("Save");

    private final List<T> _existingItems;
    private final T _client;
    private final boolean _editing;
    private final boolean _isService;

    public enum ItemType { Service, Client }


    public ClientDetailsDialog(List<T> items, T client) {
        _existingItems = items;
        _client = client;
        _isService = items.get(0) instanceof YAWLServiceReference;
        _editing = client != null;

        setHeader(getTitle());
        addComponent(createForm());
        if (_editing) {
            populateForm();
        }
        createButtons(_saveButton);
        _saveButton.setText(_editing ? "Update" : "Add");
    }


    public Button getSaveButton() { return _saveButton; }


    private String getTitle() {
        return (_client != null ? "Edit " : "Add ") +
                (_isService ? "Service" : "Client App");
    }

    
    private FormLayout createForm() {
        _nameField.setRequired(true);
        _uriField.setRequired(true);
        _passwordField.setRequired(true);
        _pwConfirmField.setRequired(true);
        _descriptionField.setRequired(true);

        _nameField.setAutocomplete(Autocomplete.OFF);
        _uriField.setAutocomplete(Autocomplete.OFF);
        _passwordField.setAutocomplete(Autocomplete.NEW_PASSWORD);
        _pwConfirmField.setAutocomplete(Autocomplete.NEW_PASSWORD);
        _descriptionField.setAutocomplete(Autocomplete.OFF);

        _nameField.focus();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_nameField, 2);
        form.add(_passwordField, 1);
        form.add(_pwConfirmField, 1);
        if (_isService) {
            form.add(_uriField, 2);
        }
        form.add(_descriptionField, 2);
        return form;
    }


    private void populateForm() {
        if (_client != null) {
            _nameField.setValue(_client.getUserName());
            _passwordField.setValue(_client.getPassword());
            _pwConfirmField.setValue(_client.getPassword());
            _descriptionField.setValue(_client.getDocumentation());
            if (_isService) {
                _uriField.setValue(((YAWLServiceReference) _client).getServiceID());
            }
        }
    }


    public YClient composeClient() {
        String userName = _nameField.getValue();
        String password = _passwordField.getValue();
        String desc = _descriptionField.getValue();
        String uri = _uriField.getValue();
        return _isService ?
                new YAWLServiceReference(uri, null, userName, password, desc) :
                new YExternalClient(userName, password, desc);
    }

    
    public boolean validate() {

        // no short circuits
        return validateName() & validateDescription() & validatePassword() & validateURI();
    }


    private boolean validateName() {
        _nameField.setInvalid(false);
        if (_nameField.isEmpty()) {
            _nameField.setErrorMessage("A service name is required");
            _nameField.setInvalid(true);
        }
        else if (! isUniqueName(_nameField.getValue())) {
            _nameField.setErrorMessage("A service with that name is already registered");
            _nameField.setInvalid(true);
        }
        return !_nameField.isInvalid();
    }


    private boolean validateDescription() {
        _descriptionField.setInvalid(false);
        if (_descriptionField.isEmpty()) {
            _descriptionField.setErrorMessage("A service description is required");
            _descriptionField.setInvalid(true);
        }
        return !_descriptionField.isInvalid();
    }


    private boolean validatePassword() {
        _passwordField.setInvalid(false);
        _pwConfirmField.setInvalid(false);
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


    private boolean validateURI() {
        _uriField.setInvalid(false);
        if (_isService) {
            if (_uriField.isEmpty()) {
                _uriField.setErrorMessage("A URI is required");
                _uriField.setInvalid(true);
            }
            else if (!isUniqueURI(_uriField.getValue())) {
                _uriField.setErrorMessage("A service is already registered at that URI");
                _uriField.setInvalid(true);

            }
            else {
                String msg = HttpURLValidator.validate(_uriField.getValue());
                if (! (msg.startsWith("<success") || msg.contains("404"))) {
                    _uriField.setErrorMessage("Invalid URI: " + StringUtil.unwrap(msg));
                    _uriField.setInvalid(true);
                }
            }
            return !_uriField.isInvalid();
        }
        return true;
    }

    private boolean isUniqueName(String name) {
        for (YClient client : _existingItems) {
            if (name.equals(client.getUserName())) {
                if (_editing && client.equals(_client)) {
                    continue;                             // editing & no name change
                }
                return false;
            }
        }
        return true;
    }


    private boolean isUniqueURI(String uri) {
        for (YClient client : _existingItems) {
            YAWLServiceReference service = (YAWLServiceReference) client;
            if (uri.equals(service.getURI())) {
                if (_editing && service.equals(_client)) {
                    continue;                             // editing & no uri change
                }
                return false;
            }
        }
        return true;
    }

}
