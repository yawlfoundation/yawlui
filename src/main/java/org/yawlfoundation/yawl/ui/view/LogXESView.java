package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.SingleSelectSpecificationIdList;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.ParticipantFieldTransposer;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.IOException;
import java.util.*;

/**
 * @author Michael Adams
 * @date 28/10/2025
 */
public class LogXESView extends AbstractGridView<LogXESView.EventRecord> {

    private final List<YSpecificationID> _specIDList;
    private final Map<String, List<XNode>> _traceNodeMap = new HashMap<>();
    private String _xesLog;
    protected SingleSelectSpecificationIdList.Versions _versions;
    private final LogView _parent;

    private ParticipantFieldTransposer _fieldTransposer;
    private final ParticipantFieldTransposer.Field _selectedResourceFormat;
    private boolean _transposerSetup = false;

    
    public LogXESView(LogView parent, List<YSpecificationID> ids,
                      SingleSelectSpecificationIdList.Versions versions,
                      ParticipantFieldTransposer.Field selectedFormat) {
        super();
        _parent = parent;
        _versions = versions;
        _selectedResourceFormat = selectedFormat;
        _specIDList = new ArrayList<>(ids);
        build();
    }

    
    protected String getAsCSV() {
        StringBuilder builder = new StringBuilder();
        builder.append(getHeadersAsCSV());
        for (EventRecord row : getItems()) {
            builder.append(row.toCSV(_versions));
        }
        return builder.toString();
    }


    protected String getAsXES() {
        if (_versions == SingleSelectSpecificationIdList.Versions.Single) {
            return transposeXesResourceIds(_xesLog);
        }
        try {
            XNode root = xesToXNode(_xesLog);
            truncateSpecificationName(root);
            List<XNode> traceNodes = new ArrayList<>(root.getChildren("trace"));
            for (XNode traceNode : traceNodes) {
                root.removeChild(traceNode);
            }
            for (String specVersion : _traceNodeMap.keySet()) {
                List<XNode> mappedTraceNodes = _traceNodeMap.get(specVersion);
                for (XNode traceNode : mappedTraceNodes) {
                    XNode versionNode = new XNode("string");
                    versionNode.addAttribute("key", "spec:version");
                    versionNode.addAttribute("value", specVersion);
                    traceNode.insertChild(0, versionNode);
                }
                root.addChildren(mappedTraceNodes);
            }
            return transposeXesResourceIds(root.toPrettyString());
        }
        catch (IOException ioe) {
            Announcement.error(ioe.getMessage());
            return null;
        }
    }

    
    @Override
    List<LogXESView.EventRecord> getItems() {
        if (_specIDList.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<EventRecord> events = new ArrayList<>();
            for (YSpecificationID specID : _specIDList) {
                _xesLog = getResourceClient().getMergedXESLog(specID, true);
                if (!successful(_xesLog)) {
                    throw new IOException(_xesLog);
                }
                events.addAll(toEventList(_xesLog));
            }
            return events;
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    
    @Override
    void addColumns(Grid<LogXESView.EventRecord> grid) {
        grid.addColumn(LogXESView.EventRecord::getCaseID).setHeader(UiUtil.bold("Case ID"));
        grid.addColumn(LogXESView.EventRecord::getTimestamp).setHeader(UiUtil.bold("Timestamp"));
        grid.addColumn(LogXESView.EventRecord::getTaskID).setHeader(UiUtil.bold("Task ID"));
        grid.addColumn(LogXESView.EventRecord::getItemID).setHeader(UiUtil.bold("Item ID"));
        grid.addColumn(LogXESView.EventRecord::getTransition).setHeader(UiUtil.bold("Event"));
        grid.addColumn(LogXESView.EventRecord::getResourceID).setHeader(UiUtil.bold("Resource ID"));
        if (_versions == SingleSelectSpecificationIdList.Versions.All) {
            grid.addColumn(LogXESView.EventRecord::getSpecVersion)
                    .setHeader(UiUtil.bold("Spec Version"));
        }
    }

    
    @Override
    void configureComponentColumns(Grid<LogXESView.EventRecord> grid) {

    }

    @Override
    void addItemActions(LogXESView.EventRecord item, ActionRibbon ribbon) {

    }

    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.FILE_CODE, "Download as XES",
                event -> {
                    String fileName = getSpecLabel().replaceAll(" - ", "_" ) + ".xes";
                    _parent.downloadFile(fileName, getAsXES());
                });
        ribbon.add(VaadinIcon.FILE_TABLE, "Download as CSV",
                event -> {
                    String fileName = getSpecLabel().replaceAll(" - ", "_" ) + ".csv";
                    _parent.downloadFile(fileName, getAsCSV());
                });
    }
    

    @Override
    String getTitle() {
        return "All Case Events for Specification: " + getSpecLabel();
    }


    private String getSpecLabel() {
        String specLabel = "";
         if (!_specIDList.isEmpty()) {
             specLabel = _versions == SingleSelectSpecificationIdList.Versions.Single ?
                     _specIDList.get(0).toString() : _specIDList.get(0).getUri();
         }
         return specLabel;
    }


    private List<LogXESView.EventRecord> toEventList(String xesLog) throws IOException {
        ParticipantFieldTransposer.Field from = null;
        ParticipantFieldTransposer.Field to = null;
        XNode root = xesToXNode(xesLog);
        List<LogXESView.EventRecord> eventList = new ArrayList<>();
        String specVersion = extractSpecVersion(root);
        List<XNode> traceNodes = root.getChildren("trace");
        if (_versions == SingleSelectSpecificationIdList.Versions.All) {
            _traceNodeMap.put(specVersion, traceNodes);
        }
        for (XNode traceNode : traceNodes) {
            String caseID = traceNode.getChild("string").getAttributeValue("value");
            for (XNode eventNode : traceNode.getChildren("event")) {
                LogXESView.EventRecord event = new EventRecord();
                event.setSpecVersion(specVersion);
                event.setCaseID(caseID);
                for (XNode dateNode : eventNode.getChildren("date")) {
                    String key = dateNode.getAttributeValue("key");
                    if (key.equals("time:timestamp")) {
                        event.setTimestamp(dateNode.getAttributeValue("value"));
                        break;
                    }
                }
                for (XNode itemNode : eventNode.getChildren("string")) {
                    String key = itemNode.getAttributeValue("key");
                    String value = itemNode.getAttributeValue("value");
                    switch (key) {
                        case "concept:name":
                            event.setTaskID(value);
                            break;
                        case "lifecycle:transition":
                            event.setTransition(value);
                            break;
                        case "concept:instance":
                            event.setItemID(value);
                            break;
                        case "org:resource":
                            if (! _transposerSetup) setupResourceFieldTransposer(value);
                            event.setResourceID(transposeFormat(value));
                            break;
                    }
                }
                eventList.add(event);
            }
        }
        return eventList;
    }


    private XNode xesToXNode(String xesLog) throws IOException {
        if (StringUtil.isNullOrEmpty(xesLog)) {
            throw new IOException("Log is empty");
        }
        XNode root = new XNodeParser().parse(xesLog);
        if (root == null) {
            throw new IOException("Error parsing xes log");
        }
        return root;
    }


    private String extractSpecVersion(XNode root) {
        for (XNode itemNode : root.getChildren("string")) {
            String key = itemNode.getAttributeValue("key");
            if (key.equals("concept:name")) {
                String value = itemNode.getAttributeValue("value");
                return value.substring(value.lastIndexOf(' '));
            }
        }
        return "";
    }


    private void truncateSpecificationName(XNode root) {
        for (XNode node : root.getChildren("string")) {
            String key = node.getAttributeValue("key");
            if (key.equals("concept:name")) {
                String spec = node.getAttributeValue("value");
                spec = spec.substring(0, spec.indexOf(" - "));
                node.addAttribute("value", spec);
                break;
            }
        }
    }


    private void setupResourceFieldTransposer(String identifier) {
        _transposerSetup = true;                                   // call exactly once
        ParticipantFieldTransposer.Field ogFormat = identifier.startsWith("PA-") ?
                ParticipantFieldTransposer.Field.Key :
                ParticipantFieldTransposer.Field.UserID;
        if (ogFormat == _selectedResourceFormat) {
            return;
        }
        try {
            _fieldTransposer = new ParticipantFieldTransposer(
                    getResourceClient().getParticipants());
            _fieldTransposer.setFields(ogFormat, _selectedResourceFormat);
        }
        catch (Exception e) {
            Announcement.warn("Unable to transpose resource ids: " +e.getMessage());
        }
    }

    

    private String transposeFormat(String ogValue) {
        if (_fieldTransposer != null) {
            return _fieldTransposer.transpose(ogValue);
        }
        return ogValue;
    }


    private String transposeXesResourceIds(String log) {
        if (_fieldTransposer != null) {
            for (Map.Entry<String, String> entry : _fieldTransposer.getTransposeMap().entrySet()) {
                log = log.replaceAll(entry.getKey(), entry.getValue());
            }
        }
        return log;
    }


    static class EventRecord {
        String specVersion;
        String caseID;
        String timestamp;
        String taskID;
        String itemID;
        String resourceID;
        String transition;

        EventRecord() { }

        public String getSpecVersion() { return specVersion; }

        public void setSpecVersion(String version) { specVersion = version; }

        public String getCaseID() { return caseID; }

        public void setCaseID(String id) { caseID = id; }

        public String getTimestamp() { return timestamp; }

        public void setTimestamp(String t) { timestamp = t; }

        public String getTaskID() { return taskID; }

        public void setTaskID(String id) { taskID = id; }

        public String getItemID() { return itemID; }

        public void setItemID(String id) { itemID = id; }

        public String getResourceID() { return resourceID; }

        public void setResourceID(String id) { resourceID = id; }

        public String getTransition() { return transition; }

        public void setTransition(String t) { transition = t; }

        // ordering must match column order
        public String toCSV(SingleSelectSpecificationIdList.Versions versions) {
            StringBuilder builder = new StringBuilder();
            builder.append(appendCheck(caseID)).append(',');
            builder.append(appendCheck(timestamp)).append(',');
            builder.append(appendCheck(taskID)).append(',');
            builder.append(appendCheck(itemID)).append(',');
            builder.append(appendCheck(transition)).append(',');

            // resource id might include a space
            String resource = appendCheck(resourceID);
            if (resource.contains(",")) {
                resource = String.format("\"%s\"", resource);
            }
            builder.append(resource);
            
            if (versions == SingleSelectSpecificationIdList.Versions.All) {
                builder.append(',');
                builder.append(appendCheck(specVersion));
            }
            builder.append('\n');
            return builder.toString();
        }


        private String appendCheck(String item) {
            return item != null ? item :  "";
        }
        
    }
    
}
