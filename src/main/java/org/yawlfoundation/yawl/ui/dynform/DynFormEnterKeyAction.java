package org.yawlfoundation.yawl.ui.dynform;

/**
 * @author Michael Adams
 * @date 12/12/2022
 */
public enum DynFormEnterKeyAction {

    COMPLETE,
    SAVE,
    NONE;


    public static DynFormEnterKeyAction fromString(String action) {
        if (action == null || action.isEmpty()) {
            return COMPLETE;
        }

        return switch (action.toUpperCase()) {
            case "SAVE" -> SAVE;
            case "NONE" -> NONE;
            default -> COMPLETE;
        };
    }

}
