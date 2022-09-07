package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 6/9/2022
 */
public class ClientAppSubView extends AbstractClientView<YExternalClient> {

    public ClientAppSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
    }


    @Override
    String getTitle() {
        return "Client Apps";
    }


    @Override
    List<YExternalClient> getItems() {
        try {
            return getEngineClient().getClientApplications();
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }


    @Override
    void addColumns(Grid<YExternalClient> grid) {
        grid.addColumn(YExternalClient::get_userid).setHeader(
                UiUtil.bold("Name"));
        grid.addColumn(YExternalClient::get_documentation).setHeader(
                UiUtil.bold("Description"));
    }


    @Override
    void configureComponentColumns(Grid<YExternalClient> grid) { }    // none to configure

}
