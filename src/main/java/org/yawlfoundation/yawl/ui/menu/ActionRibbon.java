package org.yawlfoundation.yawl.ui.menu;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * @author Michael Adams
 * @date 16/12/20
 */
//@CssImport("./styles/shared-styles.css")
public class ActionRibbon extends HorizontalLayout {

    public ActionRibbon() {
        super();
    }


    public ActionIcon add(VaadinIcon iconName, String hoverColor, String tooltip,
                    ComponentEventListener<ClickEvent<Icon>> clickListener) {
        ActionIcon icon = new ActionIcon(iconName, hoverColor, tooltip, clickListener);
        this.add(icon);
        return icon;
    }


    public ActionIcon add(int index, VaadinIcon iconName, String hoverColor, String tooltip,
                    ComponentEventListener<ClickEvent<Icon>> clickListener) {
        ActionIcon icon = new ActionIcon(iconName, hoverColor, tooltip, clickListener);
        this.addComponentAtIndex(index, icon);
        return icon;
    }


    // adds a disabled icon
    public ActionIcon add(VaadinIcon iconName) {
        ActionIcon icon = new ActionIcon(iconName);
        this.add(icon);
        return icon;
    }


    // adds a disabled icon
    public ActionIcon add(int index, VaadinIcon iconName) {
        ActionIcon icon = new ActionIcon(iconName);
        this.addComponentAtIndex(index, icon);
        return icon;
    }


    public void reset() {
        getChildren().forEach(i -> ((ActionIcon) i).reset());
    }

}
