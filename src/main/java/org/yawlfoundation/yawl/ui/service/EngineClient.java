package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.Marshaller;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceA.InterfaceA_EnvironmentBasedClient;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceB_EnvironmentBasedClient;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 11/11/20
 */
public class EngineClient extends AbstractClient {

    private final InterfaceA_EnvironmentBasedClient _iaClient = new InterfaceA_EnvironmentBasedClient(
            "http://localhost:8080/yawl/ia");

    private final InterfaceB_EnvironmentBasedClient _ibClient = new InterfaceB_EnvironmentBasedClient(
            "http://localhost:8080/yawl/ib");


    public EngineClient() { super(); }


    public List<YExternalClient> getClientApplications() throws IOException {
        return new ArrayList<>(_iaClient.getClientAccounts(getHandle()));
    }


    public List<RunningCase> getRunningCases() throws IOException {
        String casesStr = _ibClient.getAllRunningCases(getHandle());
        if (_ibClient.successful(casesStr)) {
            XNode node = new XNodeParser().parse(StringUtil.unwrap(casesStr));
            ArrayList<RunningCase> caseList = new ArrayList<>();
           if (node != null) {
               for (XNode specNode : node.getChildren()) {
                   YSpecificationID specID = new YSpecificationID(
                           specNode.getAttributeValue("id"),
                           specNode.getAttributeValue("version"),
                           specNode.getAttributeValue("uri"));
                   for (XNode caseID : specNode.getChildren()) {
                       caseList.add(new RunningCase(specID, caseID.getText()));
                   }
               }
           }
           return caseList;
        }
        return Collections.emptyList();
    }


    public UploadResult uploadSpecification(String content) throws IOException {
        String msg = _iaClient.uploadSpecification(content, getHandle());
        if (!_iaClient.successful(msg)) {
            throw new IOException(StringUtil.unwrap(msg));
        }
        return new UploadResult(msg);
    }


    public boolean unloadSpecification(YSpecificationID specID) throws IOException {
        String msg = _iaClient.unloadSpecification(specID, getHandle());
        if (!_iaClient.successful(msg)) {
            throw new IOException(StringUtil.unwrap(msg));
        }
        announceEvent(ClientEvent.Action.SpecificationUnload, specID);
        return true;
    }


    public String launchCase(YSpecificationID specID, String caseData) throws IOException {
        String msg = _ibClient.launchCase(specID, caseData, null, getHandle());
        if (!_iaClient.successful(msg)) {
            throw new IOException(StringUtil.unwrap(msg));
        }
        announceEvent(ClientEvent.Action.LaunchCase, msg);
        return msg;
    }


    public String launchCase(YSpecificationID specID, String caseData, long msecs) throws IOException {
        String msg = _ibClient.launchCase(specID, caseData, getHandle(), null, null, msecs);
        if (!_iaClient.successful(msg)) {
            throw new IOException(StringUtil.unwrap(msg));
        }
        announceEvent(ClientEvent.Action.LaunchCase, msg);
        return msg;
    }


    public void cancelCase(String caseID) throws IOException {
        _ibClient.cancelCase(caseID, getHandle());
    }


    public boolean canCreateNewInstance(String itemID) throws IOException {
        return successful(_ibClient.checkPermissionToAddInstances(itemID, getHandle()));
    }

    public WorkItemRecord createNewInstance(String itemID, String paramValue)
            throws IOException {
        String xml = _ibClient.createNewInstance(itemID, paramValue, getHandle());
        if (successful(xml)) {
            return Marshaller.unmarshalWorkItem(xml);
        }
        else throw new IOException(xml);
    }


    public TaskInformation getTaskInformation(WorkItemRecord wir) throws IOException {
        return getTaskInformation(new YSpecificationID(wir), wir.getTaskID());
    }


    public TaskInformation getTaskInformation(YSpecificationID specID, String taskID)
            throws IOException {
        String xml = _ibClient.getTaskInformationStr(specID, taskID, getHandle());
        return successful(xml) ? _ibClient.parseTaskInformation(xml) : null;
    }


    private boolean successful(String xml) { return _ibClient.successful(xml); }


    @Override
    protected void connect() throws IOException {
        if (! connected()) {
            _handle = _iaClient.connect("admin", "YAWL");
        }
    }


    @Override
    protected boolean connected() throws IOException {
        return _handle != null && _iaClient.successful(_iaClient.checkConnection(_handle));
    }


    @Override
    public void disconnect() throws IOException {
        connect();
        _iaClient.disconnect(_handle);
    }
    
}
