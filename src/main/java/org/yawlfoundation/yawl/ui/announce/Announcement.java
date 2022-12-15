/*
 * Copyright (c) 2022 Queensland University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.yawlfoundation.yawl.ui.announce;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.yawlfoundation.yawl.ui.util.ApplicationProperties;

/**
 * @author Michael Adams
 * @date 10/5/2022
 */
public class Announcement {

    private static final Notification.Position DEFAULT_POSITION = Notification.Position.TOP_END;
    private static final int DEFAULT_TIME = 5000;
    private static final boolean SUPPRESS_SUCCESS_NOTIFICATIONS =
            ApplicationProperties.suppressSuccessNotifications();


    public static Notification show(String msg) {
        return Notification.show(msg, DEFAULT_TIME, DEFAULT_POSITION);
    }


    public static Notification show(String msg, NotificationVariant variant) {
        Notification notification = Notification.show(msg, DEFAULT_TIME, DEFAULT_POSITION);
        notification.addThemeVariants(variant);
        return notification;
    }


    public static Notification success(String msg) {
        if (! SUPPRESS_SUCCESS_NOTIFICATIONS) {
            return show(msg, NotificationVariant.LUMO_SUCCESS);
        }
        return null;
    }


    public static Notification success(String formatMsg, Object... values) {
        return success(String.format(formatMsg, values));
    }


    public static Notification highlight(String msg) {
        return show(msg, NotificationVariant.LUMO_PRIMARY);
    }


    public static Notification highlight(String formatMsg, Object... values) {
        return highlight(String.format(formatMsg, values));
    }


    public static Notification warn(String msg) {
        return highlight(msg);
    }


    public static Notification warn(String formatMsg, Object... values) {
        return warn(String.format(formatMsg, values));
    }


    public static void error(String msg) {
        new ErrorMsg(stripXMLTags(msg), DEFAULT_POSITION).open();
    }


    public static void error(String formatMsg, Object... values) {
        new ErrorMsg(String.format(stripXMLTags(formatMsg), values), DEFAULT_POSITION).open();
    }


    private static String stripXMLTags(String msg) {
        return msg.replaceAll("</*\\w+>", "");
    }
    
}
