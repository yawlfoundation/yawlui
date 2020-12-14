package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.YSpecificationID;

/**
 * @author Michael Adams
 * @date 7/12/20
 */
public class RunningCase {

    private final YSpecificationID _specID;
    private final String _caseID;

    public RunningCase(YSpecificationID specID, String caseID) {
        _specID = specID;
        _caseID = caseID;
    }


    public String getCaseID() { return _caseID; }

    public String getSpecName() { return _specID.getUri(); }

    public String getSpecVersion() { return _specID.getVersionAsString(); }

}
