package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class ServicesView extends VerticalLayout {

    private final ResourceClient _resClient;
    private final EngineClient _engClient;
    private final List<YAWLServiceReference> _services;
    private final List<YExternalClient> _clients;
    
    private Grid<YAWLServiceReference> _servicesGrid;
    private Grid<YExternalClient> _clientsGrid;


    public ServicesView(ResourceClient resClient, EngineClient engClient) {
        _resClient = resClient;
        _engClient = engClient;
        _services = getRegisteredServices();
        _clients = getClientApplications();
        add(createServicesPanel());
        add(createClientsPanel());
        setSizeFull();
    }


    public void addClient(YClient client) {
        try {
            _resClient.addClient(client);
            if (client instanceof YAWLServiceReference) {
                _services.add((YAWLServiceReference) client);
                _servicesGrid.getDataProvider().refreshAll();
            }
            else {
                _clients.add((YExternalClient) client);
                _clientsGrid.getDataProvider().refreshAll();
            }
        }
        catch (IOException ioe) {
            new MessageDialog("ERROR: " + ioe.getMessage()).open();
        }
    }


    private VerticalLayout createServicesPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(new H3("Registered Custom Services"));
        panel.add(createServicesGrid());
        return panel;
    }


    private VerticalLayout createClientsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(new H3("Client Applications"));
        panel.add(createClientsGrid());
        return panel;
    }


    private Grid<YAWLServiceReference> createServicesGrid() {
        _servicesGrid = new Grid<>(YAWLServiceReference.class);
        _servicesGrid.setItems(_services);
        _servicesGrid.setColumns("_serviceName", "_documentation", "_yawlServiceID");
        _servicesGrid.getColumnByKey("_serviceName").setHeader("Name");
        _servicesGrid.getColumnByKey("_documentation").setHeader("Description");
        _servicesGrid.getColumnByKey("_yawlServiceID").setHeader("URI");
        configureGrid(_servicesGrid);
        return _servicesGrid;
    }


    private Grid<YExternalClient> createClientsGrid() {
        _clientsGrid = new Grid<>(YExternalClient.class);
        _clientsGrid.setItems(_clients);
        _clientsGrid.setColumns("_userid", "_documentation");
        _clientsGrid.getColumnByKey("_userid").setHeader("Name");
        _clientsGrid.getColumnByKey("_documentation").setHeader("Description");
        configureGrid(_clientsGrid);
        return _clientsGrid;
    }


    private void configureGrid(Grid<? extends YClient> grid) {
        Grid.Column<? extends YClient> delColumn = grid.addComponentColumn(
                item -> createDeleteButton(grid, item));
        GridUtil.configureComponentColumn(delColumn);
        grid.setHeightByRows(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        GridUtil.initialSort(grid, 0);
        createFooter(grid);
    }


    private void createFooter(Grid<? extends YClient> grid) {
        Button addButton = new Button("Add ...", event -> {
            new ClientDetailsForm(this, grid, null).open();
        });

        FooterRow footerRow = grid.appendFooterRow();
        footerRow.getCell(grid.getColumns().get(0)).setComponent(addButton);
    }


    private Button createDeleteButton(Grid<? extends YClient> grid, YClient client) {
        Icon icon = new Icon(VaadinIcon.CLOSE_SMALL);
        icon.setColor("red");
        Button b = new Button(icon, event -> {     
            String caller = (client instanceof YExternalClient) ? "Client App " : "Service ";
            String msg = "Delete " + caller + client.getUserName() + ". Are you sure?";
            ConfirmDialog dialog = new ConfirmDialog(msg);
            dialog.addOKClickListener(okEvent -> {
                removeClient(client);
                grid.getDataProvider().refreshAll();
            });
            dialog.open();
        });
        return b;
    }


    private void removeClient(YClient client) {
        try {
            _resClient.removeClient(client);
            if (client instanceof YAWLServiceReference) {
                _services.remove(client);
            }
            else if (client instanceof YExternalClient) {
                _clients.remove(client);
            }
        }
        catch (IOException ioe) {
            new MessageDialog("ERROR: " + ioe.getMessage()).open();
        }
    }


    private List<YAWLServiceReference> getRegisteredServices() {
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
            new MessageDialog("ERROR: " + e.getMessage()).open();
            return Collections.emptyList();
        }
    }


    private List<YExternalClient> getClientApplications() {
        try {
            return _engClient.getClientApplications();
        }
        catch (IOException e) {
            new MessageDialog("ERROR: " + e.getMessage()).open();
            return Collections.emptyList();
        }
    }

    
}
