package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 2/11/20
 */
public class UserMgtView extends VerticalLayout {

    public UserMgtView(final ResourceClient client) {
        Grid<Participant> grid = new Grid<>(Participant.class);
        grid.setItems(getParticipants(client));
        grid.setColumns("firstName", "lastName", "userID", "administrator");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        add(grid);
        setSizeFull();
    }

    private List<Participant> getParticipants(ResourceClient client) {
        try {
            return client.getParticipants();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }
}
