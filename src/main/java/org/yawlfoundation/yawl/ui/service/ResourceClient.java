package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.Marshaller;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.UserPrivileges;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayClientAdapter;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceLogGatewayClient;
import org.yawlfoundation.yawl.resourcing.rsInterface.WorkQueueGatewayClientAdapter;
import org.yawlfoundation.yawl.util.PasswordEncryptor;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceClient {

    private final ResourceGatewayClientAdapter _resAdapter = new ResourceGatewayClientAdapter(
            "http://localhost:8080/resourceService/gateway");

    private final WorkQueueGatewayClientAdapter _wqAdapter = new WorkQueueGatewayClientAdapter(
            "http://localhost:8080/resourceService/workqueuegateway");

    private final ResourceLogGatewayClient _logClient = new ResourceLogGatewayClient(
            "http://localhost:8080/resourceService/logGateway");


    private String _handle;


    public ResourceClient() { }


    public QueueSet getAdminWorkQueues() throws IOException, ResourceGatewayException {
        return _wqAdapter.getAdminQueues(getHandle());
    }


    public QueueSet getUserWorkQueues(String pid) throws IOException, ResourceGatewayException {
        return _wqAdapter.getParticipantQueues(pid, getHandle());
    }


    public WorkItemRecord getItem(String itemID) throws IOException, ResourceGatewayException {
        String xml = _wqAdapter.getWorkItem(itemID, getHandle());
        return Marshaller.unmarshalWorkItem(xml);
    }


    public void offerItem(String itemID, Set<String> pidSet)
            throws IOException, ResourceGatewayException {
        _wqAdapter.offerItem(pidSet, itemID, getHandle());
    }


    public void allocateItem(String itemID, String pid)
            throws IOException, ResourceGatewayException {
        _wqAdapter.allocateItem(pid, itemID, getHandle());
    }


    public void startItem(String itemID, String pid)
            throws IOException, ResourceGatewayException {
        _wqAdapter.startItem(pid, itemID, getHandle());
    }


    public void reofferItem(String itemID, Set<String> pidSet)
            throws IOException, ResourceGatewayException {
        _wqAdapter.reofferItem(pidSet, itemID, getHandle());
    }


    public void reallocateItem(String itemID, String pid)
            throws IOException, ResourceGatewayException {
        _wqAdapter.reallocateItem(pid, itemID, getHandle());
    }


    public void restartItem(String itemID, String pid)
            throws IOException, ResourceGatewayException {
        _wqAdapter.restartItem(pid, itemID, getHandle());
    }


    public void acceptItem(String itemID, String pid)
                throws IOException, ResourceGatewayException {
        _wqAdapter.acceptOffer(pid, itemID, getHandle());
    }


    public void suspendItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.suspendItem(pid, itemID, getHandle());
    }


    public void unsuspendItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.unsuspendItem(pid, itemID, getHandle());
    }


    public void chainCase(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.chainCase(pid, itemID, getHandle());
    }


    public void skipItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.skipItem(pid, itemID, getHandle());
    }


    public void delegateItem(String itemID, String pidFrom, String pidTo)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.delegateItem(pidFrom, pidTo, itemID, getHandle());
    }


    public void deallocateItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.deallocateItem(pid, itemID, getHandle());
    }


    public void reallocateItem(String itemID, String pidFrom, String pidTo, boolean stateful)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.reallocateItem(pidFrom, pidTo, itemID, stateful, getHandle());
    }


    public void pileItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.pileItem(pid, itemID, getHandle());
    }


    public void completeItem(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
        _wqAdapter.completeItem(pid, itemID, getHandle());
    }


    public void newInstance(String itemID, String pid)
                    throws IOException, ResourceGatewayException {
 //       _wqAdapter.addInstance(pid, itemID, getHandle());     //todo add to api
    }


    public Set<Participant> getAssignedParticipants(String itemID, int queue)
            throws IOException, ResourceGatewayException {
        return _wqAdapter.getParticipantsAssignedWorkItem(itemID, queue, getHandle());
    }

    public List<Participant> getParticipants() throws IOException, ResourceGatewayException {
        return _resAdapter.getParticipants(getHandle());
    }


    public Participant getParticipant(String userName) throws IOException, ResourceGatewayException {
        if (userName == null) {
            throw new ResourceGatewayException("User name cannot be null.");
        }
        if (userName.equals("admin")) {
            return null;
        }
        return _resAdapter.getParticipantFromUserID(userName, getHandle());
    }


    public UserPrivileges getUserPrivileges(String pid) throws IOException, ResourceGatewayException {
        return _wqAdapter.getUserPrivileges(pid, getHandle());
    }


    public TaskPrivileges getTaskPrivileges(String itemID) throws IOException, ResourceGatewayException {
        return _wqAdapter.getTaskPrivileges(itemID, getHandle());
    }


    public List<YAWLServiceReference> getRegisteredServices() throws IOException {
        return new ArrayList<>(_wqAdapter.getRegisteredServices(getHandle()));
    }


    public void addClient(YClient client) throws IOException {
        if (client instanceof YAWLServiceReference) {
            _wqAdapter.addRegisteredService((YAWLServiceReference) client, getHandle());
        }
        else if (client instanceof YExternalClient) {
            _wqAdapter.addExternalClient((YExternalClient) client, getHandle());
        }
    }


    public void removeClient(YClient client) throws IOException {
        if (client instanceof YAWLServiceReference) {
            _wqAdapter.removeRegisteredService(((YAWLServiceReference) client).getServiceID(), getHandle());
        }
        else if (client instanceof YExternalClient) {
            _wqAdapter.removeExternalClient(client.getUserName(), getHandle());
        }
    }


    public Set<Participant> getSubordinateParticpants(String pid)
            throws IOException, ResourceGatewayException {
        return _wqAdapter.getReportingToParticipant(pid, getHandle());
    }


    public boolean authenticate(String userName, String password)
            throws IOException, ResourceGatewayException, NoSuchAlgorithmException {
        String result = _resAdapter.validateUserCredentials(userName, PasswordEncryptor.encrypt(password),
                false, getHandle());
        return _resAdapter.successful(result);
    }


    public List<SpecificationData> getLoadedSpecificationData() throws IOException {
        return new ArrayList<>(_wqAdapter.getSpecList(_handle));
    }


    public String getMergedXESLog(YSpecificationID specID, boolean withData) throws IOException {
        return _logClient.getMergedXESLog(specID.getIdentifier(),
                specID.getVersionAsString(), specID.getUri(), withData, getHandle());
    }


    private String getHandle() throws IOException {
        connect();
        return _handle;
    }

    private void connect() {
        if (! connected()) {
            _handle = _resAdapter.connect("admin", "YAWL");
        }
    }


    private boolean connected() {
        return _handle != null && _resAdapter.checkConnection(_handle);
    }

    public void disconnect() {
        connect();
        _resAdapter.disconnect(_handle);
    }
    
}
