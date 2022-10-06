package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.util.XNode;

/**
 * @author Michael Adams
 * @date 26/9/2022
 */
public class ChainedCase {

    private final YSpecificationID _specID;
    private final String _caseID;


    public ChainedCase(YSpecificationID specID, String caseID) {
        _specID = specID;
        _caseID = caseID;
    }


    public ChainedCase(XNode node) {
        if (node != null) {
            _specID = new YSpecificationID(node.getChild("specificationid"));
            _caseID = node.getChildText("caseid");
        }
        else {
            _specID = null;
            _caseID = null;
        }
    }


    public YSpecificationID getSpecID() { return _specID; }

    public String getCaseID() { return _caseID; }


    public String toString() {
        return _specID != null ? String.format("%s :: %s v.%s", _caseID, _specID.getUri(),
                _specID.getVersionAsString()) : null;
    }

}
