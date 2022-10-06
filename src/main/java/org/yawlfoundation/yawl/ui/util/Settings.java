package org.yawlfoundation.yawl.ui.util;

/**
 * @author Michael Adams
 * @date 28/9/2022
 */
public class Settings {

    private static boolean ADMIN_ACTIONS_DIRECTLY_TO_CURRENT_USER = false;


    public static void setDirectlyToMe(boolean b) {
        ADMIN_ACTIONS_DIRECTLY_TO_CURRENT_USER = b;
    }


    public static boolean isDirectlyToMe() {
        return ADMIN_ACTIONS_DIRECTLY_TO_CURRENT_USER;
    }

}
