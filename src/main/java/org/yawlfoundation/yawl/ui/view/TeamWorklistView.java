package org.yawlfoundation.yawl.ui.view;

import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/9/2022
 */
public class TeamWorklistView extends AbstractTeamView {

    public TeamWorklistView(Participant participant) {
        this(participant, true);
    }


    public TeamWorklistView(Participant participant, boolean showHeader) {
        super(participant, showHeader);
    }


    @Override
    protected Set<Participant> getTeamMembers(Participant p) {
        try {
            return getResourceClient().getReportingTo(p.getID());
        }
        catch (IOException | ResourceGatewayException e) {
            if (e.getMessage().contains("no participants reporting to")) {
                Announcement.highlight("You don't have anyone reporting to you");
            }
            else {
                Announcement.warn(StringUtil.unwrap(e.getMessage()));
            }
        }
        return Collections.emptySet();
    }




}
