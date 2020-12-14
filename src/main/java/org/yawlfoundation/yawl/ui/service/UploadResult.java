package org.yawlfoundation.yawl.ui.service;

import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 11/12/20
 */
public class UploadResult {

    List<YSpecificationID> _specs;
    List<String> _warnings;
    List<String> _errors;

    public UploadResult(String xml) {
        parse(xml);
    }

    public boolean hasWarnings() { return !_warnings.isEmpty(); }

    public boolean hasErrors() { return !_errors.isEmpty(); }

    public boolean hasMessages() { return hasWarnings() || hasErrors(); }

    public boolean hasSpecIDs() { return !_specs.isEmpty(); }


    public List<String> getWarnings() { return _warnings; }

    public List<String> getErrors() { return _errors; }

    public List<YSpecificationID> getSpecIDs() { return _specs; }


    private void parse(String xml) {
        XNode root = new XNodeParser().parse(xml);
        if (root != null) {
            _specs = parseSpecIDs(root.getChild("specifications"));
            XNode msgNode = root.getChild("verificationMessages");
            _warnings = parseMessages(msgNode, "warning");
            _errors = parseMessages(msgNode, "error");
        }
    }


    private List<YSpecificationID> parseSpecIDs(XNode specNode) {
        if (specNode.hasChildren()) {
            List<YSpecificationID> idList = new ArrayList<>();
            for (XNode idNode : specNode.getChildren()) {
                idList.add(new YSpecificationID(idNode));
            }
            return idList;
        }
        return Collections.emptyList();
    }


    private List<String> parseMessages(XNode msgNode, String label) {
        if (msgNode.hasChildren(label)) {
            List<String> msgList = new ArrayList<>();
            for (XNode child : msgNode.getChildren(label)) {
                msgList.add(child.getChildText("message"));
            }
            return msgList;
        }
        return Collections.emptyList();
    }
    
}
