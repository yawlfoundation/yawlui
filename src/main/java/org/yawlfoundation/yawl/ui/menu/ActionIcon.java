package org.yawlfoundation.yawl.ui.menu;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * @author Michael Adams
 * @date 8/8/2022
 */
public class ActionIcon extends Icon {

    public static final String ENABLED_COLOR = "gray";
    public static final String DISABLED_COLOR = "#D5D5D7";

    private final boolean enabled;


    // creates a disabled action icon
    public ActionIcon(VaadinIcon iconName) {
        super((iconName));
        init(DISABLED_COLOR);
        enabled = false;
    }


    // creates an enabled action icon
    public ActionIcon(VaadinIcon iconName, String hoverColor, String tooltip,
                            ComponentEventListener<ClickEvent<Icon>> clickListener) {
        super((iconName));
        init(ENABLED_COLOR);
        enabled = true;
        getStyle().set("cursor", "pointer");
        getElement().setAttribute("title", tooltip);
        addMouseOutListener(ENABLED_COLOR);
        addMouseOverListener(hoverColor);
        if (clickListener != null) {
            addClickListener(clickListener);
        }
    }


    public boolean isEnabled() { return enabled; }


    public void reset() { setColor(ENABLED_COLOR); }


    private void init(String color) {
        setSize("18px");
        setColor(color);
        getStyle().set("margin-left", "4px");
    }


    private void addMouseOutListener(String color) {
        getElement().addEventListener("mouseout", event -> setColor(color));
    }


    private void addMouseOverListener(String hoverColor) {
        getElement().addEventListener("mouseover", e -> setColor(hoverColor));
    }

}
