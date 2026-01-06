package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.engine.interfce.ServletUtils;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.DrawerMenu;
import org.yawlfoundation.yawl.ui.service.Clients;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.BuildInformation;

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
public class MainView extends AppLayout implements HasDynamicTitle,
        ComponentEventListener<Tabs.SelectedChangeEvent>, AppShellConfigurator {

    private static final Map<Participant, String> _customFormHandleMap = new HashMap<>();

    private Participant _user;
    private boolean _isDrawerOpen = false;
    private final DrawerToggle _menuIcon = new DrawerToggle();
    
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


    @Override
    public String getPageTitle() {
        return "YAWL " + new BuildInformation().getUIProperties().version;
    }


    // notification from DrawerMenu of an item selection
    @Override
    public void onComponentEvent(Tabs.SelectedChangeEvent event) {
        Tab tab = event.getSelectedTab();
        if (tab == null) return;               // tab is null when user first logs on only
        switch (tab.getLabel()) {
            case "My Worklist" : setContent(new UserWorklistView(_user,
                    getCustomformHandle(_user), this)); break;
            case "My Profile" : setContent(new ProfileView(_user)); break;
            case "My Team's Worklist" : setContent(chooseGroupView()); break;
            case "Case Mgt" : setContent(new CasesView()); break;
            case "Admin Worklist" : setContent(new AdminWorklistView(_user)); break;
            case "Participants" : setContent(new ParticipantsView()); break;
            case "Org Data" : setContent(new OrgDataView()); break;
            case "Non-Human Resources" : setContent(new NonHumanResourcesView()); break;
            case "Services / Clients" : setContent(new ServicesView()); break;
            case "Log Viewer" : setContent(new LogView()); break;
            case "Calendar" : setContent(new CalendarView(_user)); break;
            case "Worklet Admin" : setContent(new WorkletAdminView()); break;
            case "About" : setContent(new AboutView()); break;
            case "Logout" : event.getSource().setSelectedIndex(0); exit();
        }
    }


    public void showGeoFormView(DynForm dynForm) {
        _isDrawerOpen = isDrawerOpened();
        setDrawerOpened(false);
        _menuIcon.setEnabled(false);
        setContent(new GeoFormView(dynForm));
    }


    public void closeGeoFormView() {
        setContent(new UserWorklistView(_user, getCustomformHandle(_user), this));
        setDrawerOpened(_isDrawerOpen);
        _menuIcon.setEnabled(true);
    }

    
    private void showLoginForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        Image logo = new Image("icons/logo5unversioned.png", "YAWL Logo");
        logo.setWidth("370px");
        layout.add(logo);

        Label version = new Label("version " + new BuildInformation().getUIProperties().version);
        version.getStyle().set("color", "gray");
        version.getStyle().set("font-size", "12px");
        version.getStyle().set("font-style", "italic");
        version.getStyle().set("text-align", "right");
        layout.add(version);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle(null);
        LoginForm login = new LoginForm(i18n);
        login.setForgotPasswordButtonVisible(false);
        login.addLoginListener(e -> authenticate(e, login));

        layout.add(login);
        layout.setSizeFull();
        setContent(layout);
    }

    
    private void authenticate(AbstractLogin.LoginEvent event, LoginForm login) {
        try {
            String username = event.getUsername();
            String password = event.getPassword();
            ResourceClient resClient = Clients.getResourceClient();
            if (resClient.authenticate(username, password)) {
                _user = resClient.getParticipant(username);
                if (_user != null) {              // authenticated but null == admin
                    _user.setUserPrivileges(resClient.getUserPrivileges(_user.getID()));
                    _customFormHandleMap.put(_user,
                            resClient.getUserCustomFormHandle(username, password));
                }
                createTitleBar(username);
                DrawerMenu menu = createMenuBar();
                setDrawerOpened(true);
                _menuIcon.setEnabled(true);
                addWorkletServiceChangeListener(menu);
            }
            else {
                setErrorMessage(login, null);        // sets default error msg
                login.setError(true);                            // show the error
            }
        }
        catch (ResourceGatewayException re) {
            setErrorMessage(login, null);
             login.setError(true);
        }
        catch (IOException | NoSuchAlgorithmException ex) {
            setErrorMessage(login, ex.getMessage());
            login.setError(true);
        }
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


    private void createTitleBar(String userName) {
        addToNavbar(_menuIcon);
        addLogo();
        addLogout(userName);
    }


    private DrawerMenu createMenuBar() {
        DrawerMenu menu = new DrawerMenu(_user);
        menu.addSelectedChangeListener(this);
        addToDrawer(menu);
        menu.selectInitialItem();  
        return menu;
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
    private void addLogout(String userName) {
        Div div = new Div();
        Label userLabel = new Label(userName);
        userLabel.getStyle().set("padding-right", "15px");
        ActionIcon logout = new ActionIcon(VaadinIcon.SIGN_OUT_ALT, null,
                "Logout", e -> exit());
        div.add(userLabel, logout);
        div.getStyle().set("margin-left", "auto");
        div.getStyle().set("padding", "15px");
        addToNavbar(div);
    }


    // update menu if worklet service is added or removed
    private void addWorkletServiceChangeListener(DrawerMenu menu) {
        Clients.getResourceClient().addEventListener(e -> {
            if (e.getObject() instanceof YAWLServiceReference &&
                    ((YAWLServiceReference) e.getObject()).getURI()
                            .contains("workletService")) {

                switch (e.getAction()) {
                    case ServiceAdd : menu.insertWorkletItem(); break;
                    case ServiceRemove : menu.removeWorkletItem(); break;
                }
            }
        });
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
                return new GroupWorklistTabbedView(_user);
            }
            else if (up.canViewTeamItems()) {
                return new TeamWorklistView(_user);
            }
            else if (up.canViewOrgGroupItems()) {
                return new OrgGroupWorklistView(_user);
            }
        }
        return new Div();
    }


    private void exit() {
        try {
            _customFormHandleMap.remove(_user);
            _user = null;
            if (_customFormHandleMap.isEmpty()) {             // if no-one's logged on
                Clients.getResourceClient().disconnect();
                Clients.getEngineClient().disconnect();
            }
        }
        catch (IOException ioe) {
            // ignore
        }
        getChildren().forEach(this::remove);                    // clear content
        showLoginForm();
    }

    /**********************************************************************/

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
            ResourceClient resClient = Clients.getResourceClient();
            try {
                WorkItemRecord wir = getStartedItem(wirID, handle, resp); // check valid item
                if (wir == null) return;                          // error resp sent
                switch (method) {
                    case "item":
                        result = wir.toXML();
                        break;
                    case "parameters":
                        result = resClient.getWorkItemParameters(wirID, handle); break;
                    case "outputOnlyParameters":
                        result = resClient.getWorkItemOutputOnlyParameters(wirID, handle); break;
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
            ResourceClient resClient = Clients.getResourceClient();
            try {
                WorkItemRecord wir = getStartedItem(wirID, handle, resp); // check valid item
                if (wir == null) return;                          // error resp sent
                switch (method) {
                    case "save":
                        String data = req.getParameter("data");
                        result = resClient.updateWorkItemData(wirID, data, handle);
                        break;
                    case "complete":
                         resClient.completeItem(wir, getParticipantID(handle));
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
            if (! Clients.getResourceClient().isValidUserSessionHandle(handle)) {
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
                Set<WorkItemRecord> startedItems = Clients.getResourceClient()
                        .getQueuedItems(pid, WorkQueue.STARTED);
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
