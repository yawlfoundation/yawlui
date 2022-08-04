/*
 * Copyright (c) 2021 Queensland University of Technology
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

package org.yawlfoundation.yawl.ui.util;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.util.StringUtil;

/**
 * @author Michael Adams
 * @date 2/11/21
 */
public class UiUtil {

    public static final String VAADIN_BLUE = "2C68EC";
    public static final String DISABLED_COLOUR = "BDC3CC";


    public static void removeTopMargin(Component c) {
        setStyle(c,"margin-top", "0");
    }

    public static void removeTopPadding(Component c) {
        setStyle(c,"padding-top", "0");
    }

    public static void removeBottomPadding(Component c) {
        setStyle(c,"padding-bottom", "0");
    }

    public static Html bold(String text) {
        return new Html(StringUtil.wrap(text, "b"));
    }

    public static void setStyle(Component c, String key, String value) {
        c.getElement().getStyle().set(key, value);
    }


    public static void setTooltip(Component c, String tip) {
        setAttribute(c, "title", tip);
    }


    public static void setAttribute(Component c, String key, String value) {
        c.getElement().setAttribute(key, value);
    }


    public static Icon createIcon(VaadinIcon vIcon) {
        return createIcon(vIcon, VAADIN_BLUE);
    }

    public static Icon createIcon(VaadinIcon vIcon, String colour) {
        Icon icon = vIcon.create();
        icon.setSize("24px");
        icon.setColor(colour);
        return icon;
    }


    public static Button createToolButton(VaadinIcon vIcon, String tip, boolean enabled,
                                          ComponentEventListener<ClickEvent<Button>> clickEvent) {
        Icon icon = createIcon(vIcon);
        Button button = new Button(icon, clickEvent);
        button.setEnabled(enabled);
        setTooltip(button, tip);
        return button;
    }

}
