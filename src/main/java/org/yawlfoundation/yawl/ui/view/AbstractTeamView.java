package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.io.IOException;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/9/2022
 */
public abstract class AbstractTeamView extends AbstractWorklistView {

    public AbstractTeamView(ResourceClient client, Participant participant) {
        this(client, participant, true);
    }

    public AbstractTeamView(ResourceClient client, Participant participant,
                            boolean showHeader) {
        super(client, null, participant, showHeader);
    }



    abstract Set<Participant> getTeamMembers(Participant p);

    @Override
    protected QueueSet refreshQueueSet(Participant p) {
        return refreshMembersQueueSet(p);
    }


    @Override
    void addItemActions(WorkItemRecord item, ActionRibbon ribbon) {
         // no item actions
    }


    @Override
    protected String getTitle() {
        return "";
    }

    @Override
    protected boolean showHeader() {
        return super.showHeader();
    }

    @Override
    protected Grid<WorkItemRecord> createGrid() {
        Grid<WorkItemRecord> grid = createAdminGrid();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        return grid;
    }


    private QueueSet refreshMembersQueueSet(Participant p) {
        QueueSet qSet = new QueueSet();
        for (Participant member : getTeamMembers(p)) {
            try {
                QueueSet memberSet = getResourceClient().getUserWorkQueues(member.getID());
                for (WorkQueue queue : memberSet.getActiveQueues()) {
                    qSet.addToQueue(queue.getQueueType(), queue);
                }
            }
            catch (IOException | ResourceGatewayException e) {
                Announcement.warn(e.getMessage());
            }
        }
        return qSet;
    }

}
