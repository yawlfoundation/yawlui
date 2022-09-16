package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.html.Label;

/**
 * @author Michael Adams
 * @date 15/9/2022
 */
public class Prompt extends Label {

    public Prompt() {
        getStyle().set("color", "var(--lumo-secondary-text-color)");
        getStyle().set("font-weight", "500");
        getStyle().set("font-size", "var(--lumo-font-size-s)");
        getStyle().set("padding", "7px 0 5px 5px");
    }


    public Prompt(String text) {
        this();
        setText(text);
    }

}
