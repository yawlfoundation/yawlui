package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.interfaceE.YLogGatewayClient;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 22/10/2025
 */
public class LogClient extends EngineClient {

    private final YLogGatewayClient _logClient;

    public LogClient() {
        super();
        _logClient = new YLogGatewayClient(buildURI(getHost(), getPort(), "yawl/logGateway"));
    }


    public List<YSpecificationID> getAllSpecifications() throws IOException {
        String xml = _logClient.getAllSpecifications(getHandle());
        if (successful(xml)) {
            List<YSpecificationID> idList = new ArrayList<>();
            XNode specsNode = new XNodeParser().parse(xml);
            for (XNode specNode : specsNode.getChildren()) {
                idList.add(new YSpecificationID(specNode.getChild()));      // <id> tag
            }
            return idList;
        }
        else throw new IOException(StringUtil.unwrap(xml));
    }


    public List<Object> getAllCasesOfSpecification(YSpecificationID specID) throws IOException {
        String xml = _logClient.getCompleteCaseLogsForSpecification(specID.getIdentifier(),
                specID.getVersionAsString(), specID.getUri(), getHandle());
        successCheck(xml);
        return unmarshalCases(xml);
    }


    public List<Object> getCaseEvents(String caseID) throws IOException {
        String xml = _logClient.getCaseEvents(caseID, getHandle());
        successCheck(xml);
        return unmarshalCase(xml);
    }


    public List<Object> getAllCasesStartedByService(String serviceName) throws IOException {
        String xml = _logClient.getAllCasesStartedByService(serviceName, getHandle());
        successCheck(xml);
        return unmarshalCases(xml);
    }


    public List<Object> getAllCasesCancelledByService(String serviceName) throws IOException {
        String xml = _logClient.getAllCasesCancelledByService(serviceName, getHandle());
        successCheck(xml);
        return unmarshalCases(xml);
     }


    public LogStatistics getStatistics(YSpecificationID specID, long from, long to) throws IOException {
        String xml = _logClient.getSpecificationStatistics(specID, from, to, getHandle());
        successCheck(xml);
        LogStatistics stats = new LogStatistics(xml);
        stats.setFrom(from);
        stats.setTo(to);
        return stats;
    }


    private void successCheck(String xml) throws IOException {
        if (! successful(xml)) {
            throw new IOException(StringUtil.unwrap(xml));
        }
    }


    //todo
    private List<Object> unmarshalCases(String xml) {
        List<Object> caseList = new ArrayList<>();
        // parse, then foreach case make a case object
        return caseList;
    }


    //todo
    private List<Object> unmarshalCase(String xml) {
         List<Object> caseEvents = new ArrayList<>();
         // parse, then foreach case make a case object
         return caseEvents;
     }


}
