package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;


@Route("")
//@PWA(name = "Project Base for Vaadin", shortName = "Project Base", enableInstallPrompt = false)
@CssImport("./styles/shared-styles.css")
//@Theme(value = Lumo.class)
//@CssImport(value = "./styles/vaadin-text-field-styles.css", themeFor = "vaadin-text-field")
public class MainView extends AppLayout implements AuthenticationSuccessListener,
        ComponentEventListener<Tabs.SelectedChangeEvent>, PageConfigurator {

    private final ResourceClient _resClient = new ResourceClient();
    private final EngineClient _engClient = new EngineClient();
    private Participant _user;

    private Div _footer;


    public MainView() {
//        _footer = createFooter();
        showLoginPanel();
    }


    // called on startup to set the app's favicon
    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addFavIcon(InitialPageSettings.Position.PREPEND,
                "icon", "icons/yawlLogo.png", "32x32");
    }


    // notification of successful logon from LoginPanel
    @Override
    public void userAuthenticated(Participant p) {
        _user = p;
        createMenuBar(p);
        setContent(getLandingPage(p));
    }


    // notification from DrawerMenu of an item selection
    @Override
    public void onComponentEvent(Tabs.SelectedChangeEvent event) {
        Tab tab = event.getSelectedTab();

        // tab's element's root has two children: an icon and a span (i.e. the label)
        String label = tab.getElement().getChild(0).getChild(1).getText();
        switch (label) {
            case "My Worklist" : setContent(new UserMgtView(_resClient)); break;
            case "My Profile" : setContent(null); break;
            case "My Team's Worklist" : setContent(null); break;
            case "Case Mgt" : setContent(new CasesView(_resClient, _engClient)); break;
            case "Admin Worklist" : setContent(null); break;
            case "Resources" : setContent(null); break;
            case "Org Data" : setContent(null); break;
            case "Non-Human Resources" : setContent(null); break;
            case "Calendar" : setContent(null); break;
            case "Clients"   : setContent(new ServicesView(_resClient, _engClient)); break;
            case "About" : setContent(null); break;
            case "Exit" : event.getSource().setSelectedIndex(0); exit();
        }
    }


    public Participant getCurrentUser() { return _user; }


    private void createMenuBar(Participant p) {
        Image img = new Image("icons/yawlLogo.png", "YAWL Logo");
        img.setHeight("44px");
        addToNavbar(new DrawerToggle(), img);
        DrawerMenu menu = new DrawerMenu(p);
        menu.addSelectedChangeListener(this);
        addToDrawer(menu);
    }


    private Div createFooter() {
        Div tempContent = new Div();
        tempContent.setText("temp content");
        setContent(tempContent);
        Div footer = new Div();
        footer.addClassName("app-footer");
        int lastIndex = getElement().getChildCount();
        footer.setWidthFull();
        footer.setText("YAWL v5.0");
        getElement().insertChild(lastIndex, footer.getElement());
        return footer;
    }


    private void showLoginPanel() {
        LoginPanel loginPanel = new LoginPanel(_resClient);
        loginPanel.addAuthenticationSuccessListener(this);
    }


    private Component getLandingPage(Participant p) {
        return p == null ? new AdminWorklistView(_resClient) : new WorklistView(_resClient);
    }


    private void exit() {
        try {
            _resClient.disconnect();
            _engClient.disconnect();
        }
        catch (IOException ioe) {
            // ignore
        }
        showLoginPanel();
    }
    
}
