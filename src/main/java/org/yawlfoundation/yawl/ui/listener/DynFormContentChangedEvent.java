package org.yawlfoundation.yawl.ui.listener;

/**
 *
 * @author Michael Adams
 * @date 17/12/2025
 */
public class DynFormContentChangedEvent {
    private final String varName;
    private final String panelName;
    private final String fieldName;
    private final String dataType;
    private final String oldValue;
    private final String newValue;

    public DynFormContentChangedEvent(String varName, String panelName, String fieldName,
                                      String dataType, String oldValue, String newValue) {
        this.varName = varName;
        this.panelName = panelName;
        this.fieldName = fieldName;
        this.dataType = dataType;
        this.oldValue = oldValue;
        this.newValue = newValue;
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
}
