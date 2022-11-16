package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import org.yawlfoundation.yawl.engine.interfce.ServletUtils;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.DrawerMenu;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Route("")
//@PWA(name = "Project Base for Vaadin", shortName = "Project Base", enableInstallPrompt = false)
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@Theme("common-theme")
public class MainView extends AppLayout implements
        ComponentEventListener<Tabs.SelectedChangeEvent>, AppShellConfigurator {

    private static final ResourceClient _resClient = new ResourceClient();
    private static final EngineClient _engClient = new EngineClient();
    private Participant _user;
    private Div _footer;

    public static Map<Participant, String> _customFormHandleMap = new HashMap<>();


    public MainView() {
        super();
        showLoginForm();
    }


    // called on startup to set the app's favicon
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon(Inline.Position.PREPEND,
                "icon", "icons/favicon.png", "32x32");
    }


    // notification from DrawerMenu of an item selection
    @Override
    public void onComponentEvent(Tabs.SelectedChangeEvent event) {
        Tab tab = event.getSelectedTab();          
        switch (tab.getLabel()) {
            case "My Worklist" : setContent(new UserWorklistView(
                    _resClient, _engClient, _user, getCustomformHandle(_user))); break;
            case "My Profile" : setContent(new ProfileView(_resClient, _user)); break;
            case "My Team's Worklist" : setContent(chooseGroupView()); break;
            case "Case Mgt" : setContent(new CasesView(_resClient, _engClient)); break;
            case "Admin Worklist" : setContent(new AdminWorklistView(_resClient, _user)); break;
            case "Participants" : setContent(new ParticipantsView(_resClient)); break;
            case "Org Data" : setContent(new OrgDataView(_resClient)); break;
            case "Non-Human Resources" : setContent(new NonHumanResourcesView(_resClient)); break;
            case "Calendar" : setContent(new CalendarView(_resClient, _user)); break;
            case "Services / Clients" : setContent(new ServicesView(_resClient, _engClient)); break;
            case "About" : setContent(null); break;
            case "Logout" : event.getSource().setSelectedIndex(0); exit();
        }
    }


    private void showLoginForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        Image logo = new Image("icons/logo5.png", "YAWL Logo");
        logo.setWidth("500px");
        layout.add(logo);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle(null);
        LoginForm login = new LoginForm(i18n);
        login.setForgotPasswordButtonVisible(false);
        login.addLoginListener(e -> {
            try {
                String username = e.getUsername();
                String password = e.getPassword();
                if (_resClient.authenticate(username, password)) {
                    _user = _resClient.getParticipant(username);
                    _user.setUserPrivileges(_resClient.getUserPrivileges(_user.getID()));
                    _customFormHandleMap.put(_user,
                            _resClient.getUserCustomFormHandle(username, password));
                    createMenuBar(_user);
                }
                else {
                    setErrorMessage(login, null);        // sets default error msg
                    login.setError(true);                            // show the error
                }
            }
            catch (ResourceGatewayException | IOException |
                   NoSuchAlgorithmException ex) {
                setErrorMessage(login, ex.getMessage());
                login.setError(true);
            }
        });


        layout.add(login);
        layout.setSizeFull();
        setContent(layout);
    }


    private String getCustomformHandle(Participant user) {
        return _customFormHandleMap.get(user);
    }

    // the vaadin way to set the error msg to other than the default
     private void setErrorMessage(LoginForm login, String message) {
         LoginI18n i18n = LoginI18n.createDefault();             // resets to default msg
         if (message != null) {
             i18n.getErrorMessage().setMessage(message);
         }
         login.setI18n(i18n);
     }


    public Participant getCurrentUserSessionHandle() { return _user; }


    private void createMenuBar(Participant p) {
        addToNavbar(new DrawerToggle());
        addLogo();
        addLogout();
        DrawerMenu _menu = new DrawerMenu(p);
        _menu.addSelectedChangeListener(this); 
        addToDrawer(_menu);
//        setDrawerOpened(false);
        _menu.selectInitialItem();
    }


    private void addLogo() {
        Div div = new Div();
        Image img = new Image("icons/logo5unversioned.png", "YAWL Logo");
        img.setHeight("56px");
        div.add(img);
        div.getStyle().set("margin-left", "auto");
        div.getStyle().set("display", "block");
        addToNavbar(div);
    }

    // add a logout 'button' on the right side of the nav bar
    private void addLogout() {
        Div div = new Div();
        ActionIcon logout = new ActionIcon(VaadinIcon.SIGN_OUT_ALT, null,
                "Logout", e -> exit());
        div.add(logout);
        div.getStyle().set("margin-left", "auto");
        div.getStyle().set("padding", "15px");
        addToNavbar(div);
    }


    private Div createFooter() {
        Div footer = new Div();
        footer.addClassName("app-footer");
        int lastIndex = getElement().getChildCount();
        footer.setWidthFull();
        footer.setText("YAWL v5.0");
        getElement().insertChild(lastIndex, footer.getElement());
        return footer;
    }


    private Component chooseGroupView() {
        if (_user != null) {
            UserPrivileges up = _user.getUserPrivileges();
            if (_user.isAdministrator() || (
                    up.canViewOrgGroupItems() && up.canViewOrgGroupItems())) {
                return new GroupWorklistTabbedView(_resClient, _user);
            }
            else if (up.canViewTeamItems()) {
                return new TeamWorklistView(_resClient, _user);
            }
            else if (up.canViewOrgGroupItems()) {
                return new OrgGroupWorklistView(_resClient, _user);
            }
        }
        return new Div();
    }


    private void exit() {
        try {
            _customFormHandleMap.remove(_user);
            _user = null;
            if (_customFormHandleMap.isEmpty()) {             // if no-one's logged on
                _resClient.disconnect();
                _engClient.disconnect();
            }
        }
        catch (IOException ioe) {
            // ignore
        }
        getChildren().forEach(this::remove);                    // clear content
        showLoginForm();
    }


    // This inner class serves custom form endpoints
    @WebServlet("/customform/*")
    public static class CustomFormServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String[] parts = parseRequest(req, resp);
            if (parts == null) return;                             // error resp sent
            String handle = parts[3];
            String wirID = parts[4];
            String method = parts.length == 6 ? parts[5] : "item";
            String result = null;
            try {
                WorkItemRecord wir = getStartedItem(wirID, handle, resp); // check valid item
                if (wir == null) return;                          // error resp sent
                switch (method) {
                    case "item":
                        result = wir.toXML();
                        break;
                    case "parameters":
                        result = _resClient.getWorkItemParameters(wirID, handle); break;
                    case "outputOnlyParameters":
                        result = _resClient.getWorkItemOutputOnlyParameters(wirID, handle); break;
                    default:
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unrecognized resource");
                        break;
                }
            }
            catch (ResourceGatewayException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            send(resp, result);
        }


        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String[] parts = parseRequest(req, resp);
            if (parts == null) return;                             // error resp sent
            String handle = parts[3];
            String wirID = parts[4];
            String method = parts.length == 6 ? parts[5] : "save";
            String result = null;
            try {
                WorkItemRecord wir = getStartedItem(wirID, handle, resp); // check valid item
                if (wir == null) return;                          // error resp sent
                switch (method) {
                    case "save":
                        String data = req.getParameter("data");
                        result = _resClient.updateWorkItemData(wirID, data, handle);
                        break;
                    case "complete":
                         _resClient.completeItem(wir, getParticipantID(handle));
                         break;
                    default:
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unrecognized resource");
                        break;
                }
            }
            catch (ResourceGatewayException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }

            send(resp, result);
        }


        private String[] parseRequest(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {

            // "", "yawlui", "customform", user-level handle, item id, endpoint (opt)
            String[] parts = req.getRequestURI().split("/");
            if (parts.length < 5) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed request URL");
                return null;
            }

            String handle = parts[3];
            if (! _resClient.isValidUserSessionHandle(handle)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid session handle");
                return null;
            }

            return parts;
        }


        private void send(HttpServletResponse resp, String result) throws IOException {
            if (result != null) {
                 OutputStreamWriter outputWriter = ServletUtils.prepareResponse(resp);
                 ServletUtils.finalizeResponse(outputWriter, result);
             }
        }


        private WorkItemRecord getStartedItem(String wirID, String handle, HttpServletResponse resp)
                throws ResourceGatewayException, IOException {
            String pid = getParticipantID(handle);
            if (pid != null) {
                Set<WorkItemRecord> startedItems = _resClient.getQueuedItems(
                        pid, WorkQueue.STARTED);
                for (WorkItemRecord item : startedItems) {
                    if (item.getID().equals(wirID)) {
                        return item;
                    }
                }
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unrecognized resource");
            return null;
        }


        private String getParticipantID(String userHandle) {
            for (Participant user : _customFormHandleMap.keySet()) {
                String handle = _customFormHandleMap.get(user);
                if (handle.equals(userHandle)) {
                    return user.getID();
                }
            }
            return null;
        }

    }

}
