package org.yawlfoundation.yawl.ui.view;

import org.yawlfoundation.yawl.resourcing.resource.Participant;

/**
 * @author Michael Adams
 * @date 3/11/20
 */
public interface AuthenticationSuccessListener {

    void userAuthenticated(Participant p);
}
