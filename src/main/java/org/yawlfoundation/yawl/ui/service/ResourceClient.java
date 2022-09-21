package org.yawlfoundation.yawl.ui.service;

import org.jdom2.Element;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.authentication.YExternalClient;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.Marshaller;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.QueueSet;
import org.yawlfoundation.yawl.resourcing.TaskPrivileges;
import org.yawlfoundation.yawl.resourcing.resource.*;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayClientAdapter;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceLogGatewayClient;
import org.yawlfoundation.yawl.resourcing.rsInterface.WorkQueueGatewayClientAdapter;
import org.yawlfoundation.yawl.ui.util.TaskPrivilegesCache;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.PasswordEncryptor;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ResourceClient extends AbstractClient {

    private final ResourceGatewayClientAdapter _resAdapter = new ResourceGatewayClientAdapter(
            "http://localhost:8080/resourceService/gateway");

    private final WorkQueueGatewayClientAdapter _wqAdapter = new WorkQueueGatewayClientAdapter(
            "http://localhost:8080/resourceService/workqueuegateway");

    private final ResourceLogGatewayClient _logClient = new ResourceLogGatewayClient(
            "http://localhost:8080/resourceService/logGateway");

    public final TaskPrivilegesCache _taskPrivilegesCache = new TaskPrivilegesCache(this);


    public ResourceClient() { super(); }


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


    public void completeItem(WorkItemRecord wir, String pid)
                    throws IOException, ResourceGatewayException {

        // have to put output data on the server first
        Element e = wir.getUpdatedData() != null ? wir.getUpdatedData() : wir.getDataList();
        String data = JDOMUtil.elementToStringDump(e);
        _wqAdapter.updateWorkItemData(wir.getID(), data, getHandle());
        
        _wqAdapter.completeItem(pid, wir.getID(), getHandle());
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


    public boolean isOrgDataModifiable() {
        try {
            return _resAdapter.isOrgDataSetModifiable(getHandle());
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addParticipant(Participant p) throws IOException {
        return _resAdapter.addParticipant(p, true, getHandle());
    }

    public String updateParticipant(Participant p) throws IOException {
        return _resAdapter.updateParticipant(p, true, getHandle());
    }

    public String deleteParticipant(Participant p) throws IOException {
        return _resAdapter.removeParticipant(p, getHandle());
    }

    public List<AbstractResourceAttribute> getRoles()
            throws IOException, ResourceGatewayException {
        return _resAdapter.getRoles(getHandle());
    }

    public String addParticipantToRole(String p, String r) throws IOException {
        return _resAdapter.getClient().addParticipantToRole(p, r, getHandle());
    }

    public String addParticipantToCapability(String p, String c) throws IOException {
        return _resAdapter.getClient().addParticipantToCapability(p, c, getHandle());
    }

    public String addParticipantToPosition(String p, String pos) throws IOException {
        return _resAdapter.getClient().addParticipantToPosition(p, pos, getHandle());
    }

    public String removeParticipantFromRole(String p, String r) throws IOException {
        return _resAdapter.getClient().removeParticipantFromRole(p, r, getHandle());
    }

    public String removeParticipantFromCapability(String p, String c) throws IOException {
        return _resAdapter.getClient().removeParticipantFromCapability(p, c, getHandle());
    }

    public String removeParticipantFromPosition(String p, String pos) throws IOException {
        return _resAdapter.getClient().removeParticipantFromPosition(p, pos, getHandle());
    }

    public List<AbstractResourceAttribute> getCapabilities()
            throws IOException, ResourceGatewayException {
        return _resAdapter.getCapabilities(getHandle());
    }

    public List<AbstractResourceAttribute> getPositions()
            throws IOException, ResourceGatewayException {
        return _resAdapter.getPositions(getHandle());
    }

    public List<AbstractResourceAttribute> getOrgGroups()
            throws IOException, ResourceGatewayException {
        return _resAdapter.getOrgGroups(getHandle());
    }

    public Role getRole(String rid) throws IOException, ResourceGatewayException {
        return _resAdapter.getRole(rid, getHandle());
    }

    public Position getPosition(String pid) throws IOException, ResourceGatewayException {
        return _resAdapter.getPosition(pid, getHandle());
    }

    public OrgGroup getOrgGroup(String oid) throws IOException, ResourceGatewayException {
        return _resAdapter.getOrgGroup(oid, getHandle());
    }


    public List<AbstractResourceAttribute> getParticipantRoles(String pid)
            throws IOException, ResourceGatewayException {
        return _resAdapter.getParticipantRoles(pid, getHandle());
    }

    public List<AbstractResourceAttribute> getParticipantCapabilities(String pid)
            throws IOException, ResourceGatewayException {
        return _resAdapter.getParticipantCapabilities(pid, getHandle());
    }

    public List<AbstractResourceAttribute> getParticipantPositions(String pid)
            throws IOException, ResourceGatewayException {
        return _resAdapter.getParticipantPositions(pid, getHandle());
    }

    public List<Participant> getRoleMembers(String name)
            throws IOException, ResourceGatewayException {
        Set<Participant> pSet = _resAdapter.getParticipantsWithRole(name, getHandle());
        return pSet != null ? new ArrayList<>(pSet) : Collections.emptyList();
    }

    public List<Participant> getCapabilityMembers(String name)
            throws IOException, ResourceGatewayException {
        Set<Participant> pSet = _resAdapter.getParticipantsWithCapability(name, getHandle());
        return pSet != null ? new ArrayList<>(pSet) : Collections.emptyList();
    }

    public List<Participant> getPositionMembers(String name)
            throws IOException, ResourceGatewayException {
        Set<Participant> pSet = _resAdapter.getParticipantsWithPosition(name, getHandle());
        return pSet != null ? new ArrayList<>(pSet) : Collections.emptyList();
    }
    
    public Set<Participant> getReportingTo(String pid)
            throws IOException, ResourceGatewayException {
        return _wqAdapter.getReportingToParticipant(pid, getHandle());
    }


    public List<Participant> getOrgGroupMembers(String oid)
            throws IOException, ResourceGatewayException {
        return new ArrayList<>(_wqAdapter.getOrgGroupMembers(oid, getHandle()));
    }

    public UserPrivileges getUserPrivileges(String pid)
            throws IOException, ResourceGatewayException {
        return _wqAdapter.getUserPrivileges(pid, getHandle());
    }

    public String setUserPrivileges(Participant p) throws IOException {
        return _resAdapter.setParticipantPrivileges(p, getHandle());
    }


    public Role addRole(Role role) throws IOException {
        String id = _resAdapter.addRole(role, getHandle());
        if (successful(id)) {
            role.setID(id);
            return role;
        }
        throw new IOException(StringUtil.unwrap(id));
    }

    public Capability addCapability(Capability capability) throws IOException {
        String id = _resAdapter.addCapability(capability, getHandle());
        if (successful(id)) {
            capability.setID(id);
            return capability;
        }
        throw new IOException(StringUtil.unwrap(id));
    }

    public Position addPosition(Position position) throws IOException {
        String id = _resAdapter.addPosition(position, getHandle());
        if (successful(id)) {
            position.setID(id);
            return position;
        }
        throw new IOException(StringUtil.unwrap(id));
    }

    public OrgGroup addOrgGroup(OrgGroup orgGroup) throws IOException {
        String id = _resAdapter.addOrgGroup(orgGroup, getHandle());
        if (successful(id)) {
            orgGroup.setID(id);
            return orgGroup;
        }
        throw new IOException(StringUtil.unwrap(id));
    }

    public String updateRole(Role role) throws IOException {
        return _resAdapter.updateRole(role, getHandle());
    }

    public String updateCapability(Capability capability) throws IOException {
        return _resAdapter.updateCapability(capability, getHandle());
    }

    public String updatePosition(Position position) throws IOException {
        return _resAdapter.updatePosition(position, getHandle());
    }

    public String updateOrgGroup(OrgGroup orgGroup) throws IOException {
        return _resAdapter.updateOrgGroup(orgGroup, getHandle());
    }

    public String removeRole(Role role) throws IOException {
        return _resAdapter.removeRole(role, getHandle());
    }

    public String removeCapability(Capability capability) throws IOException {
        return _resAdapter.removeCapability(capability, getHandle());
    }

    public String removePosition(Position position) throws IOException {
        return _resAdapter.removePosition(position, getHandle());
    }

    public String removeOrgGroup(OrgGroup orgGroup) throws IOException {
        return _resAdapter.removeOrgGroup(orgGroup, getHandle());
    }


    public TaskPrivileges getTaskPrivileges(WorkItemRecord wir)
            throws IOException, ResourceGatewayException {
        TaskPrivileges privileges = _taskPrivilegesCache.get(wir);
        if (privileges == null) {
            privileges = _wqAdapter.getTaskPrivileges(wir.getID(), getHandle());
            if (privileges != null) {
                _taskPrivilegesCache.put(wir, privileges);
            }
        }
        return privileges;
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


    public String importOrgData(String xml) throws IOException {
        return _resAdapter.importOrgData(xml, getHandle());
    }

    public String exportOrgData() throws IOException {
        return _resAdapter.exportOrgData(getHandle());
    }


    public boolean successful(String msg) { return _resAdapter.successful(msg); }


    @Override
    protected void connect() {
        if (! connected()) {
            _handle = _resAdapter.connect("admin", "YAWL");
        }
    }


    @Override
    protected boolean connected() {
        return _handle != null && _resAdapter.checkConnection(_handle);
    }


    @Override
    public void disconnect() {
        connect();
        _resAdapter.disconnect(_handle);
    }
    
}
