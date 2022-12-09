package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.tabs.Tab;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.List;

/**
 * @author Michael Adams
 * @date 19/9/2022
 */
public class GroupWorklistTabbedView extends AbstractTabbedView {

    private Tab _tabTeam;
    private Tab _tabGroup;


    public GroupWorklistTabbedView(Participant participant) {
        super(participant);
    }


    @Override
    protected List<Tab> getTabs() {
        if (_tabTeam == null) _tabTeam = new Tab("Team");
        if (_tabGroup == null) _tabGroup = new Tab("Org Group");
        return List.of(_tabTeam, _tabGroup);
    }


    @Override
    void setContent(Tab tab) {
        if (tab.equals(_tabTeam)) {
            getContent().add(new TeamWorklistView(getParticipant(), false));
        }
        else {
            getContent().add(new OrgGroupWorklistView(getParticipant(), false));
        }
    }
    
}
