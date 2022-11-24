package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;
import org.yawlfoundation.yawl.worklet.admin.AdministrationTask;
import org.yawlfoundation.yawl.worklet.support.WorkletGatewayClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 11/11/20
 */
public class WorkletClient extends AbstractClient {

    private final WorkletGatewayClient _wsClient = new WorkletGatewayClient();


    public WorkletClient() { super(); }


    public AdministrationTask getWorkletAdministrationTask(int id) throws IOException {
        String xml = _wsClient.getAdministrationTask(id, getHandle());
        return newAdministrationTask(xml);
    }


    public List<AdministrationTask> getWorkletAdministrationTasks() throws IOException {
        String xml = _wsClient.getAdministrationTasks(getHandle());
        if (successful(xml)) {
            XNode node = new XNodeParser().parse(xml);
            if (node != null) {
                List<AdministrationTask> taskList = new ArrayList<>();
                node.getChildren().forEach(child -> {
                    AdministrationTask task = new AdministrationTask();
                    task.fromXNode(child);
                    taskList.add(task);
                });
                return taskList;
            }
            throw new IOException("Unable to retrieve administration tasks: malformed XML");
        }
        throw new IOException(StringUtil.unwrap(xml));
    }


    public AdministrationTask addWorkletAdministrationTask(AdministrationTask task)
            throws IOException {
        String xml = _wsClient.addAdministrationTask(task.getCaseID(), task.getItemID(),
                task.getTitle(), task.getScenario(), task.getProcess(), task.getTaskType(),
                getHandle());
        return newAdministrationTask(xml);
    }


    public void removeWorkletAdministrationTask(int id)
            throws IOException {
        String xml = _wsClient.removeAdministrationTask(id, getHandle());
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
    }


    public void raiseCaseExternalException(String caseID, String trigger) throws IOException {
        String xml = _wsClient.raiseCaseExternalException(caseID, trigger, getHandle());
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
    }


    public void raiseItemExternalException(String itemID, String trigger) throws IOException {
        String xml = _wsClient.raiseItemExternalException(itemID, trigger, getHandle());
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
    }


    public List<String> getExternalTriggersForCase(String caseID) throws IOException {
        String xml = _wsClient.getExternalTriggersForCase(caseID, getHandle());
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
        return xmlToStringList(xml);
    }

    
    public List<String> getExternalTriggersForItem(String itemID) throws IOException {
        String xml = _wsClient.getExternalTriggersForItem(itemID, getHandle());
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
        return xmlToStringList(xml);
    }

    
    private AdministrationTask newAdministrationTask(String xml) throws IOException {
        if (successful(xml)) {
            XNode node = new XNodeParser().parse(xml);
            if (node != null) {
                AdministrationTask task = new AdministrationTask();
                task.fromXNode(node);
                return task;
            }
            throw new IOException("Unable to retrieve administration task: malformed XML");
        }
        throw new IOException(StringUtil.unwrap(xml));
    }


    private List<String> xmlToStringList(String xml) throws IOException {
        XNode node = new XNodeParser().parse(xml);
        if (node == null) {
            throw new IOException("Unable to retrieve administration tasks: malformed XML");
        }

        List<String> list = new ArrayList<>();
        node.getChildren().forEach(child -> list.add(child.getText()));
        return list;
    }


    
    private boolean successful(String xml) { return _wsClient.successful(xml); }


    @Override
    protected void connect() throws IOException {
        if (! connected()) {
            _handle = _wsClient.connect("admin", "YAWL");
        }
    }


    @Override
    protected boolean connected() throws IOException {
        return _handle != null && successful(_handle) &&
                successful(_wsClient.checkConnection(_handle));
    }


    @Override
    public void disconnect() throws IOException {
        connect();
        _wsClient.disconnect(_handle);
    }
    
}
