package org.yawlfoundation.yawl.ui.view;

import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Position;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/9/2022
 */
public class OrgGroupWorklistView extends AbstractTeamView {

    public OrgGroupWorklistView(ResourceClient client, Participant participant) {
        this(client, participant, true);
    }

    public OrgGroupWorklistView(ResourceClient client, Participant participant,
                                boolean showHeader) {
        super(client, participant, showHeader);
    }

    @Override
    protected Set<Participant> getTeamMembers(Participant p) {
        Set<Participant> teamMembers = new HashSet<>();

        try {
            for (Position pos : p.getPositions()) {
                String oid = pos.get_orgGroupID();
                if (oid != null) {
                    teamMembers.addAll(getResourceClient().getOrgGroupMembers(oid));
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
