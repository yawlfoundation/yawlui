package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.util.XNode;

/**
 * @author Michael Adams
 * @date 26/9/2022
 */
public class PiledTask {

    private final YSpecificationID _specID;
    private final String _taskID;


    public PiledTask(YSpecificationID specID, String taskID) {
        _specID = specID;
        _taskID = taskID;
    }


    public PiledTask(XNode node) {
        if (node != null) {
            _specID = new YSpecificationID(node.getChild("specificationid"));
            _taskID = node.getChildText("taskid");
        }
        else {
            _specID = null;
            _taskID = null;
        }
    }


    public YSpecificationID getSpecID() { return _specID; }

    public String getTaskID() { return _taskID; }


    public String toString() {
        return _specID != null ? String.format("%s v.%s :: %s", _specID.getUri(),
                _specID.getVersionAsString(), _taskID) : null;
    }

}
