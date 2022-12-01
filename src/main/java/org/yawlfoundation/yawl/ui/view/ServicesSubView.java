package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.ui.announce.Announcement;
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

/**
 * @author Michael Adams
 * @date 6/9/2022
 */
public class ServicesSubView extends AbstractClientView<YAWLServiceReference> {

    private Grid.Column<YAWLServiceReference> _connectedColumn;


    public ServicesSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
    }


    @Override
    String getTitle() {
        return "Services";
    }


    @Override
    List<YAWLServiceReference> getItems() {
        try {
            List<YAWLServiceReference> services =
                    getResourceClient().getRegisteredServices();

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


    @Override
    void addColumns(Grid<YAWLServiceReference> grid) {
        _connectedColumn = grid.addComponentColumn(this::connectedIndicator);
        grid.addColumn(YAWLServiceReference::getServiceName).setHeader(
                UiUtil.bold("Name"));
        grid.addColumn(YAWLServiceReference::get_documentation).setHeader(
                UiUtil.bold("Description"));
        grid.addColumn(YAWLServiceReference::getURI).setHeader(
                UiUtil.bold("URI"));
    }


    @Override
    void configureComponentColumns(Grid<YAWLServiceReference> grid) {
        configureActionColumn(_connectedColumn);
        initialSort(grid, 1);
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

}
