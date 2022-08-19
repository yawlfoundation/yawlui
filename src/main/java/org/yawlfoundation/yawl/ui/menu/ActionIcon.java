package org.yawlfoundation.yawl.ui.menu;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * @author Michael Adams
 * @date 8/8/2022
 */
public class ActionIcon extends Icon {

    public static final String DEFAULT_HOVER  = "#275ED3";             // blue
    public static final String RED = "#AF2318";
    public static final String GREEN = "#009926";
    public static final String ENABLED_COLOR = "gray";
    public static final String DISABLED_COLOR = "#D5D5D7";

    private DomListenerRegistration mouseOverListener;
    private DomListenerRegistration mouseOutListener;
    private Registration addedClickListener;
    private ComponentEventListener<ClickEvent<Icon>> clickListener;
    private String hoverColor;
    private String tooltip;
    private final boolean canToggleState;
    private boolean enabled;


    // creates a disabled action icon
    public ActionIcon(VaadinIcon iconName) {
        super((iconName));
        init(DISABLED_COLOR);
        enabled = false;
        canToggleState = false;
    }


    // creates an enabled action icon
    public ActionIcon(VaadinIcon iconName, String hoverColor, String tooltip,
                            ComponentEventListener<ClickEvent<Icon>> listener) {
        super((iconName));
        init(ENABLED_COLOR);
        canToggleState = true;
        this.tooltip = tooltip;
        this.hoverColor = hoverColor != null ? hoverColor : DEFAULT_HOVER;
        clickListener = listener;
        setEnabled(true);
    }


    public boolean isEnabled() { return enabled; }


    public void reset() { setColor(isEnabled() ? ENABLED_COLOR : DISABLED_COLOR); }


    public void setEnabled(boolean enable) {
        if (! canToggleState || enabled == enable) return;  // can't change or no change
        enabled = enable;
        setColor(enable ? ENABLED_COLOR : DISABLED_COLOR);
        if (enable) {
            getStyle().set("cursor", "pointer");
            getElement().setAttribute("title", tooltip);
            addMouseOutListener(ENABLED_COLOR);
            addMouseOverListener(hoverColor);
            if (clickListener != null) {
                addedClickListener = addClickListener(clickListener);
            }
        }
        else {
            getStyle().remove("cursor");
            getElement().removeAttribute("title");
            if (mouseOutListener != null) {
                mouseOutListener.remove();
            }
            if (mouseOverListener != null) {
                mouseOverListener.remove();
            }
            if (addedClickListener != null) {
                addedClickListener.remove();
            }
        }
    }


    private void init(String color) {
        setSize("18px");
        setColor(color);
        getStyle().set("margin-left", "4px");
    }


    private void addMouseOutListener(String color) {
        Element e = getElement();
        mouseOutListener = e.addEventListener("mouseout",
                event -> setColor(color));
    }


    private void addMouseOverListener(String hoverColor) {
        Element e = getElement();
        mouseOverListener = e.addEventListener("mouseover",
                event -> setColor(hoverColor));
    }

}
