package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.util.HttpURLValidator;
import org.yawlfoundation.yawl.util.StringUtil;

import java.util.Collection;

/**
 * @author Michael Adams
 * @date 12/11/20
 */
public class ClientDetailsForm extends Dialog {

    private final TextField _nameField = new TextField("Name");
    private final TextField _uriField = new TextField("URI");
    private final PasswordField _passwordField = new PasswordField("Password");
    private final PasswordField _pwConfirmField = new PasswordField("Confirm Password");
    private final TextArea _descriptionField = new TextArea("Description");

    private final ServicesView _parentView;
    private final Grid<? extends YClient> _grid;
    private final YClient _client;


    public ClientDetailsForm(ServicesView parentView, Grid<? extends YClient> grid, YClient client) {
        _parentView = parentView;
        _grid = grid;
        _client = client;

        setCloseOnOutsideClick(false);
        setWidth("700px");
        setHeight("515px");
        add(getContent());
    }


    private VerticalLayout getContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.add(getTitle());
        layout.add(createForm());
        layout.add(createButtons());
        return layout;
    }


    private H3 getTitle() {
        String title = "Edit " + (isServiceGrid() ? "Custom Service" : "Client Application");
        return new H3(title);
    }

    private FormLayout createForm() {
        _nameField.setRequired(true);
        _uriField.setRequired(true);
        _passwordField.setRequired(true);
        _pwConfirmField.setRequired(true);
        _descriptionField.setRequired(true);
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 4));
        form.add(_nameField, 4);
        form.add(_passwordField, 2);
        form.add(_pwConfirmField, 2);
        if (isServiceGrid()) {
            form.add(_uriField, 4);
        }
        form.add(_descriptionField, 4);
        return form;
    }


    private HorizontalLayout createButtons() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(new Button("Cancel", event -> {
            close();
        }));
        layout.add(new Button("Save", event -> {
            if (validate()) {
                _parentView.addClient(composeNewClient());
                close();
            }
        }));
        return layout;
    }


    private YClient composeNewClient() {
        String userName = _nameField.getValue();
        String password = _passwordField.getValue();
        String desc = _descriptionField.getValue();
        String uri = _uriField.getValue();
        return isServiceGrid() ?
            new YAWLServiceReference(uri, null, userName, password, desc) :
                new YExternalClient(userName, password, desc);
    }

    
    private boolean validate() {
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
        if (isServiceGrid()) {
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
                if (!msg.startsWith("<success")) {
                    _uriField.setErrorMessage("Invalid URI: " + StringUtil.unwrap(msg));
                    _uriField.setInvalid(true);
                }
            }
            return !_uriField.isInvalid();
        }
        return true;
    }

    private boolean isUniqueName(String name) {
        for (YClient client : getGridItems()) {
            if (name.equals(client.getUserName())) {
                return false;
            }
        }
        return true;
    }


    private boolean isUniqueURI(String uri) {
        for (YClient client : getGridItems()) {
            YAWLServiceReference service = (YAWLServiceReference) client;
            if (uri.equals(service.getURI())) {
                return false;
            }
        }
        return true;
    }


    private boolean isServiceGrid() {
        return _grid.getBeanType().equals(YAWLServiceReference.class);
    }

    
    private Collection<? extends YClient> getGridItems() {
        return ((ListDataProvider<? extends YClient>) _grid.getDataProvider()).getItems();
    }

}
