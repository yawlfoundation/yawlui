package org.yawlfoundation.yawl.ui.layout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * @author Michael Adams
 * @date 2/8/2022
 */
public class UnpaddedVerticalLayout extends VerticalLayout {

    public UnpaddedVerticalLayout() {
        super();
        setStyle("padding", "0");
    }


    public UnpaddedVerticalLayout(Component... components) {
        super(components);
        setStyle("padding", "0");
    }


    public UnpaddedVerticalLayout(String sides) {
        super();
        applySides(sides);
    }


    public UnpaddedVerticalLayout(String sides, Component... components) {
        this(sides);
        add(components);
    }


    private void applySides(String sides) {
        if (sides.contains("t")) setStyle("padding-top", "0");
        if (sides.contains("b")) setStyle("padding-bottom", "0");
        if (sides.contains("l")) setStyle("padding-left", "0");
        if (sides.contains("r")) setStyle("padding-right", "0");
    }

    private void setStyle(String k, String v) {
        getElement().getStyle().set(k, v);
    }
}
