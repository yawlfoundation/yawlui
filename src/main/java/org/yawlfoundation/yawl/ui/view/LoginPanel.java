package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 3/11/20
 */
public class LoginPanel extends LoginOverlay {

    // the main view wants to know when a user successfully logs in
    private final Set<AuthenticationSuccessListener> _successListeners = new HashSet<>();

    public LoginPanel(final ResourceClient client) {
        super();
        setForgotPasswordButtonVisible(false);
        setTitle("YAWL");
        setDescription(null);
        setOpened(true);

        addLoginListener(e -> {
            try {
                if (authenticate(client, e)) {
                    Participant p = getLoggedInUser(client, e.getUsername());
                    setOpened(false);
                    announceAuthenticationSuccess(p);
                } else {
                    setErrorMessage(null);                     // sets default error msg
                    setError(true);                            // show the error
                }
            }
            catch (ResourceGatewayException | IOException | NoSuchAlgorithmException ex) {
                setErrorMessage(ex.getMessage());
                setError(true);
                ex.printStackTrace();
            }
        });
    }


    protected void addAuthenticationSuccessListener(AuthenticationSuccessListener asl) {
        _successListeners.add(asl);
    }


    private boolean authenticate(ResourceClient client, AbstractLogin.LoginEvent e)
            throws ResourceGatewayException, NoSuchAlgorithmException, IOException {
        return client.authenticate(e.getUsername(), e.getPassword());
    }


    private Participant getLoggedInUser(ResourceClient client, String userName)
            throws IOException, ResourceGatewayException {
        return client.getParticipant(userName);
    }


    private void announceAuthenticationSuccess(Participant p) {
        for (AuthenticationSuccessListener asl : _successListeners) {
            asl.userAuthenticated(p);
        }
    }


    // the vaadin way to set the error msg to other than the default
    private void setErrorMessage(String message) {
        LoginI18n i18n = LoginI18n.createDefault();             // resets to default msg
        if (message != null) {
            i18n.getErrorMessage().setMessage(message);
        }
        setI18n(i18n);
    }
}
