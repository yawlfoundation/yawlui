package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.engine.YSpecificationID;
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
public class EngineClient {

    private final InterfaceA_EnvironmentBasedClient _iaClient = new InterfaceA_EnvironmentBasedClient(
            "http://localhost:8080/yawl/ia");

    private final InterfaceB_EnvironmentBasedClient _ibClient = new InterfaceB_EnvironmentBasedClient(
            "http://localhost:8080/yawl/ib");

    private String _handle;


    public EngineClient() { }


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
        return true;
    }

    public void cancelCase(String caseID) throws IOException {
        _ibClient.cancelCase(caseID, getHandle());
    }


    private List<YSpecificationID> toSpecIDList(String xml) throws IOException {
        XNode node = new XNodeParser().parse(xml);
        if (node == null) throw new IOException("Upload failed: unable to parse result");
        List<YSpecificationID> idList = new ArrayList<>();
        for (XNode child : node.getChildren()) {
            idList.add(new YSpecificationID(child));
        }
        return idList;
    }

    private String getHandle() throws IOException {
        connect();
        return _handle;
    }


    private void connect() throws IOException {
        if (! connected()) {
            _handle = _iaClient.connect("admin", "YAWL");
        }
    }


    private boolean connected() throws IOException {
        return _handle != null && _iaClient.successful(_iaClient.checkConnection(_handle));
    }


    public void disconnect() throws IOException {
        connect();
        _iaClient.disconnect(_handle);
    }
}
