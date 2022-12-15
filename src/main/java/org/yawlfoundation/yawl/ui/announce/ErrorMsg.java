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


import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;

/**
 * @author Michael Adams
 * @date 16/3/2022
 */
public class ErrorMsg extends Notification {
    
    public ErrorMsg(String msg) {
        super();
        addThemeVariants(NotificationVariant.LUMO_ERROR);
        setPosition(Position.MIDDLE);

        ActionIcon closeIcon = new ActionIcon(VaadinIcon.CLOSE_SMALL, "white",
                "Close", event -> close());
        closeIcon.setColor("white");
        closeIcon.getStyle().set("margin-left", "auto");

        Div text = new Div(new Text(msg));
        Icon warningIcon = VaadinIcon.WARNING.create();

        HorizontalLayout hl = new HorizontalLayout(warningIcon, closeIcon);
        hl.setWidthFull();
        VerticalLayout layout = new VerticalLayout( hl, text);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        add(layout);
    }


    public ErrorMsg(String msg, Position position) {
        this(msg);
        setPosition(position);
    }

}
