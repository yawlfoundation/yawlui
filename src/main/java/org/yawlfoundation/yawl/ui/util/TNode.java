package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.schema.internal.YInternalType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Michael Adams
 * @date 8/5/2026
 */
public class TNode {

    private final String _label;
    private final String _type;
    private final Map<String, TNode> _children = new HashMap<>();

    public TNode(String label, String type) {
        _label = label;
        _type = type;
    }


    public void addChild(String label, TNode child) {
        _children.put(label, child);
    }


    public Map<String, TNode> getChildren() {
        return _children;
    }

    
    public String getType() {
        return _type;
    }

    public String getLabel() {
        return _label;
    }


    public boolean isInternalType() {
        return YInternalType.isType(getType());
    }


    public boolean hasInternalTypeInTree() {
        return hasInternalTypeInTree(this);
    }


    public boolean hasInternalTypeInTree(TNode current) {
        if (current.isInternalType()) {     // current first
            return true;
        }

        for (TNode child : current.getChildren().values()) {
            boolean found = hasInternalTypeInTree(child);      // recurse children
            if (found) return true;
        }

        return false;
    }


    public String findTypeByPath(List<String> path) {
        TNode current = this;

        for (String label : path) {
            current = current.getChildren().get(label);
            if (current == null) {
                return null; // Path doesn't exist
            }
        }

        return current.getType();
    }

    
    public TNode findNodeByLabel(String label) {
        if (getLabel().equals(label)) return this;
        for (TNode child : getChildren().values()) {
            TNode found = child.findNodeByLabel(label);
            if (found != null) return found;
        }
        return null;
    }


    public TNode findNodeByType(String type) {
        if (getType().equals(type)) return this;
        for (TNode child : getChildren().values()) {
            TNode found = child.findNodeByType(type);
            if (found != null) return found;
        }
        return null;
    }

}
