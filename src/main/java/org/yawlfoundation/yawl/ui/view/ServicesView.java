package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.ClientDetailsDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.HttpUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.yawlfoundation.yawl.ui.dialog.ClientDetailsDialog.ItemType;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class ServicesView extends AbstractView {

    private List<YAWLServiceReference> _services;
    private List<YExternalClient> _clients;
    
    private H4 _servicesHeader;
    private H4 _clientsHeader;
    private Grid<YAWLServiceReference> _servicesGrid;
    private Grid<YExternalClient> _clientsGrid;


    public ServicesView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
        _services = getServices();
        _clients = getClientApps();
        add(createSplitView(createServicesPanel(),createClientsPanel()));
        setSizeFull();
    }


    private VerticalLayout createServicesPanel() {
        _servicesHeader = new H4("Services (" + _services.size() + ")");
        return createGridPanel(_servicesHeader, createServicesGrid());
    }


    private VerticalLayout createClientsPanel() {
        _clientsHeader = new H4("Client Apps (" + _clients.size() + ")");
        return createGridPanel(_clientsHeader, createClientsGrid());
    }


    private Grid<YAWLServiceReference> createServicesGrid() {
        _servicesGrid = new Grid<>();
        _servicesGrid.setItems(_services);
        _servicesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        Grid.Column<YAWLServiceReference> connectedColumn = _servicesGrid.addComponentColumn(
                this::connectedIndicator);
        _servicesGrid.addColumn(YAWLServiceReference::getServiceName).setHeader(
                UiUtil.bold("Name"));
        _servicesGrid.addColumn(YAWLServiceReference::get_documentation).setHeader(
                UiUtil.bold("Description"));
        _servicesGrid.addColumn(YAWLServiceReference::getURI).setHeader(
                UiUtil.bold("URI"));
        Grid.Column<YAWLServiceReference> actionColumn = _servicesGrid.addComponentColumn(
                this::createServicesActions);
        configureGrid(_servicesGrid);
        configureActionColumn(connectedColumn);
        configureActionColumn(actionColumn);
        addGridFooter(_servicesGrid, createFooterActions(_servicesGrid, _services,
                ItemType.Service));
        return _servicesGrid;
    }


    private Grid<YExternalClient> createClientsGrid() {
        _clientsGrid = new Grid<>();
        _clientsGrid.setItems(_clients);
        _clientsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        _clientsGrid.addColumn(YExternalClient::get_userid).setHeader(
                UiUtil.bold("Name"));
        _clientsGrid.addColumn(YExternalClient::get_documentation).setHeader(
                UiUtil.bold("Description"));
        Grid.Column<YExternalClient> actionColumn = _clientsGrid.addComponentColumn(
                this::createClientActions);
        configureGrid(_clientsGrid);
        configureActionColumn(actionColumn);
        addGridFooter(_clientsGrid, createFooterActions(_clientsGrid, _clients,
                ItemType.Client));
        return _clientsGrid;
    }


    private ActionRibbon createFooterActions(Grid<? extends YClient> grid,
                                             List<? extends YClient> items,
                                             ItemType itemType) {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.PLUS, "Add", event -> {
            ClientDetailsDialog dialog = new ClientDetailsDialog(items, null,
                    itemType);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    YClient client = dialog.composeClient();
                    if (addClient(client)) {
                        refresh(itemType);
                        dialog.close();
                        announceSuccess(client, "added");
                    }
                }
            });
            dialog.open();
            ribbon.reset();
        });

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove Selected",
                event -> {
                    grid.getSelectedItems().forEach(item -> {
                        if (removeClient(item)) {
                            announceSuccess(item, "removed");
                        }
                    });
                    refresh(itemType);
                });

        ribbon.add(VaadinIcon.REFRESH, "Refresh", event ->
                refresh(itemType));

        return ribbon;
    }


    private Icon connectedIndicator(YAWLServiceReference service) {
        Icon indicator = new Icon(VaadinIcon.CIRCLE);
        indicator.setSize("10px");
        indicator.getStyle().set("margin-left", "4px");
        try {
            URL serviceURL = new URL(service.getURI());
            indicator.setColor(HttpUtil.isResponsive(serviceURL) ? "#009926" : "#E60026");
        }
        catch (MalformedURLException e) {
            indicator.setColor("gray");
        }
        return indicator;
    }


    private ActionRibbon createServicesActions(YAWLServiceReference service) {
        return createActions(_services, service);
    }

    
    private ActionRibbon createClientActions(YExternalClient client) {
        return createActions(_clients, client);
    }

    
    private ActionRibbon createActions(List<? extends YClient> items, YClient client) {
        ItemType itemType = (client instanceof YAWLServiceReference) ?
                        ItemType.Service : ItemType.Client;
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
            ClientDetailsDialog dialog = new ClientDetailsDialog(items, client, itemType);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    updateClient(client, dialog.composeClient());
                    dialog.close();
                    announceSuccess(client, "updated");
                    refresh(itemType);
                }
            });
            dialog.open();
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
            if (removeClient(client)) {
                announceSuccess(client, "removed");
                refresh(itemType);
            }
        });
        
        return ribbon;
    }
    

    private void refresh(ItemType itemType) {
        if (itemType == ItemType.Service) {
            refreshServices();
        }
        else {
            refreshClients();
        }
    }


    private void refreshClients() {
        _clients = getClientApps();
        _clientsGrid.setItems(_clients);
        _clientsGrid.getDataProvider().refreshAll();
        _clientsGrid.recalculateColumnWidths();
        refreshHeader(_clientsHeader, "Client Apps", _clients.size());
    }


    private void refreshServices() {
        _services = getServices();
        _servicesGrid.setItems(_services);
        _servicesGrid.getDataProvider().refreshAll();
        _servicesGrid.recalculateColumnWidths();
        refreshHeader(_servicesHeader, "Services", _services.size());
    }


    private void announceSuccess(YClient client, String verb) {
        String msg = String.format("%s %s %s", (
                        client instanceof YAWLServiceReference ?
                                "Service" : "Client App"),
                client.getUserName(), verb);
    }


    private boolean addClient(YClient client) {
        try {
            _resClient.addClient(client);
            return true;
        }
        catch (IOException ioe) {
            Announcement.error(ioe.getMessage());
            return false;
        }
    }


    private void updateClient(YClient oldClient, YClient newClient) {
        removeClient(oldClient);
        addClient(newClient);
    }


    private boolean removeClient(YClient client) {
        try {
            _resClient.removeClient(client);
            return true;
        }
        catch (IOException ioe) {
            Announcement.error(ioe.getMessage());
            return false;
        }
    }


    private List<YAWLServiceReference> getServices() {
        try {
            List<YAWLServiceReference> services = _resClient.getRegisteredServices();

            //only include assignable services
            List<YAWLServiceReference> assignableServices = new ArrayList<>();
            for (YAWLServiceReference service : services) {
                if (service.isAssignable()) {
                    assignableServices.add(service);
                }
            }
            return assignableServices;
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }


    private List<YExternalClient> getClientApps() {
        try {
            return _engClient.getClientApplications();
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    
}
