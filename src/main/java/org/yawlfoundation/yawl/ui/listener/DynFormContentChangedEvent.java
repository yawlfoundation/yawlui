package org.yawlfoundation.yawl.ui.listener;

import org.yawlfoundation.yawl.ui.dynform.SubPanel;

/**
 *
 * @author Michael Adams
 * @date 17/12/2025
 */
public class DynFormContentChangedEvent {

    private String layoutId;
    private String parentLayoutId;
    private String varName;
    private String panelName;
    private String fieldName;
    private String dataType;
    private String oldValue;
    private String newValue;

    private SubPanel subPanel;

    private EventType eventType;

    public enum EventType {
        SUB_PANEL_ADDED,
        SUB_PANEL_REMOVED,
        VALUE_CHANGED
    }
    

    public DynFormContentChangedEvent(String id, String parentId, String varName,
                                      String panelName, String fieldName,
                                      String dataType, String oldValue, String newValue) {
        this.layoutId = id;
        this.parentLayoutId = parentId;
        this.varName = varName;
        this.panelName = panelName;
        this.fieldName = fieldName;
        this.dataType = dataType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        eventType = EventType.VALUE_CHANGED;
    }

    public DynFormContentChangedEvent(String id, SubPanel subPanel, EventType eventType) {
        this.layoutId = id;
        this.subPanel = subPanel;
        this.eventType = eventType;
    }

    public String getLayoutId() { return layoutId; }

    public String getParentLayoutId() { return parentLayoutId; }

    public EventType eventType() {
        return eventType;
    }

    public String getVarName() {
        return varName;
    }

    public String getPanelName() {
        return panelName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDataType() {
        return dataType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public SubPanel getSubPanel() { return subPanel; }
}
