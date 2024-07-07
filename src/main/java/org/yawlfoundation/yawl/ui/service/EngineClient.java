package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.Marshaller;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceA.InterfaceA_EnvironmentBasedClient;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceB_EnvironmentBasedClient;
import org.yawlfoundation.yawl.ui.util.ApplicationProperties;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.IOException;
import java.util.*;

/**
 * @author Michael Adams
 * @date 11/11/20
 */
public class EngineClient extends AbstractClient {

    private final InterfaceA_EnvironmentBasedClient _iaClient;
    private final InterfaceB_EnvironmentBasedClient _ibClient;


    public EngineClient() {
        super();
        String host = ApplicationProperties.getEngineHost();
        String port = ApplicationProperties.getEnginePort();
        _iaClient = new InterfaceA_EnvironmentBasedClient(buildURI(host, port, "yawl/ia"));
        _ibClient = new InterfaceB_EnvironmentBasedClient(buildURI(host, port, "yawl/ib"));
    }


    public List<YExternalClient> getClientApplications() throws IOException {
        Set<YExternalClient> clients = _iaClient.getClientAccounts(getHandle());
        return clients != null ? new ArrayList<>(clients) : Collections.emptyList();
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
            String wirXML = StringUtil.unwrap(xml);          // strip 'success' tags
            return Marshaller.unmarshalWorkItem(wirXML);
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


    public YSpecificationID getSpecificationIDForCase(String caseID) throws IOException {
        String xml = _ibClient.getSpecificationIDForCase(caseID, getHandle());
        if (! successful(xml))  {
            throw new IOException("Malformed specification id returned from engine");
        }
        XNode specNode = new XNodeParser().parse(xml);
        if (specNode == null)  {
            throw new IOException("Malformed specification id returned from engine");
        }
        return new YSpecificationID(specNode);
    }


    public Map<String, String> getBuildProperties() throws IOException {
        String props = _iaClient.getBuildProperties(getHandle());
        if (successful(props)) {
            return buildPropertiesToMap(StringUtil.unwrap(props));
        }
        throw new IOException("Failed to load engine build properties: " +
                StringUtil.unwrap(props));
    }


    private boolean successful(String xml) { return _ibClient.successful(xml); }


    @Override
    protected void connect() throws IOException {
        if (connected()) return;

        _handle = _iaClient.connect(getPair().left, getPair().right);
        if (! connected()) {
            _handle = _iaClient.connect(getDefaults().left, getDefaults().right);
            if (!connected()) {
                throw new IOException("Failed to connect to YAWL Engine");
            }
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
