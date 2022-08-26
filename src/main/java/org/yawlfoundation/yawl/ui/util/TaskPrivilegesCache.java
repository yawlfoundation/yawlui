package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.ui.service.ClientEvent;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 19/8/2022
 */
public class TaskPrivilegesCache {

    private final Map<YSpecificationID, Map<String, TaskPrivileges>> _cache = new HashMap<>();


    public TaskPrivilegesCache(ResourceClient client) {
        client.addEventListener(e -> {
            if (e.getAction() == ClientEvent.Action.SpecificationUnload) {
                remove((YSpecificationID) e.getObject());
            }
        });
    }


    public TaskPrivileges get(WorkItemRecord wir) {
        Map<String, TaskPrivileges> taskMap = getTaskMap(wir);
        return taskMap.get(wir.getTaskID());
    }


    public void put(WorkItemRecord wir, TaskPrivileges privileges) {
        Map<String, TaskPrivileges> taskMap = getTaskMap(wir);
        taskMap.put(wir.getTaskID(), privileges);
    }


    private Map<String, TaskPrivileges> getTaskMap(WorkItemRecord wir) {
        YSpecificationID key = new YSpecificationID(wir);
        return _cache.computeIfAbsent(key, k -> new HashMap<>());
    }


    private void remove(YSpecificationID specID) {
        _cache.remove(specID);
    }

}
