package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.WorkQueue;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Position;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/9/2022
 */
public class TeamWorklistView extends AbstractWorklistView {

    private String _displayed;


    public TeamWorklistView(ResourceClient client, Participant participant) {
        super(client, null, participant);
        if (canChooseView(participant)) {
            addRadioGroup();
        }
     }

    @Override
    protected QueueSet refreshQueueSet(Participant p) {
        if (_displayed == null) {
            initDisplayed(p);
        }
        return refreshMembersQueueSet(p);
    }


    @Override
    protected String getTitle() {
        if (_displayed != null) {
            return _displayed + "'s Work Items";
        }

        // first time
        UserPrivileges up = getParticipant().getUserPrivileges();
        if (getParticipant().isAdministrator() || up.canViewTeamItems()) {
            return "Team's Work Items";
        }
        return "Org Group's Work Items";
    }


    @Override
    protected ActionRibbon createColumnActions(WorkItemRecord wir) {
        return new ActionRibbon();
    }

    
    @Override
    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.REFRESH, "Refresh", event -> refreshGrid());
        return ribbon;
    }


    @Override
    protected Grid<WorkItemRecord> createGrid() {
        Grid<WorkItemRecord> grid = createAdminGrid();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        return grid;
    }


    private void initDisplayed(Participant p) {
        UserPrivileges up = p.getUserPrivileges();
        if (p.isAdministrator() || (up.canViewTeamItems() && up.canViewOrgGroupItems())) {
            _displayed = "Team";
        }
        else if (up.canViewOrgGroupItems()) {
            _displayed = "Org Group";
        }
        else {
            _displayed = "Team";
        }
    }


    private void addRadioGroup() {
        RadioButtonGroup<String> rbGroup = new RadioButtonGroup<>();
        rbGroup.setItems("Team", "Org Group");
        rbGroup.setValue("Team");
        rbGroup.addValueChangeListener(e -> {
            _displayed = e.getSource().getValue();
            refreshGrid();
        });
        setFooterComponent(0, rbGroup);
    }


    private boolean canChooseView(Participant p) {
        if (p.isAdministrator()) {
            return true;
        }
        UserPrivileges up = p.getUserPrivileges();
        return up.canViewTeamItems() && up.canViewOrgGroupItems();
    }


    private QueueSet refreshMembersQueueSet(Participant p) {
        QueueSet qSet = new QueueSet();
        for (Participant member : getTeamMembers(p)) {
            try {
                QueueSet memberSet = getClient().getUserWorkQueues(member.getID());
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
    

    private Set<Participant> getTeamMembers(Participant p) {
        Set<Participant> teamMembers = new HashSet<>();

        try {
            if ("Team".equals(_displayed)) {
                teamMembers.addAll(getClient().getReportingTo(p.getID()));
            }
            else {
                for (Position pos : p.getPositions()) {
                    String oid = pos.get_orgGroupID();
                    if (oid != null) {
                        teamMembers.addAll(getClient().getOrgGroupMembers(oid));
                    }
                }
            }
        }
        catch (IOException | ResourceGatewayException e) {
            if (e.getMessage().contains("no participants reporting to")) {
                Announcement.highlight("You don't have anyone reporting to you");
            }
            else {
                Announcement.warn(StringUtil.unwrap(e.getMessage()));
            }
        }
        return teamMembers;
    }


}
