package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 13/10/2022
 */
public class DynFormLayout extends FormLayout {

    private static final double FIELD_HEIGHT = 51.5;
    private static final double PADDING_HEIGHT = 22.0;
    private static final double WINDOW_HEIGHT_FRACTION = 0.65;
    private static final int COLUMN_COUNT = 2;
    public static final double HEADING_HEIGHT = 59;

    
    @Override
    public void add(Collection<Component> components) {
        super.add(components);
        components.forEach(this::setColspan);
    }


    @Override
    public void addComponentAtIndex(int index, Component component) {
        super.addComponentAtIndex(index, component);
        setColspan(component);
    }


    @Override
    public void addComponentAsFirst(Component component) {
        super.addComponentAsFirst(component);
        setColspan(component);
    }


    public String getAppropriateWidth() {
        long count = getChildren().count();
        if (count < 2) {
            long subpanels = getChildren().filter(
                    component -> (component instanceof SubPanel)).count();
            if (subpanels == 0) {
                return "350px";               // only a single non-subPanel component
            }
        }
        return "700px";
    }


    public void setAppropriateHeight(int windowHeight) {
        double height = Math.min(windowHeight * WINDOW_HEIGHT_FRACTION, calculateHeight());
        setHeight(height + "px");
    }

    
    private void setColspan(Component component) {
        int colspan = (component instanceof SubPanel) ? COLUMN_COUNT : 1;
        super.setColspan(component, colspan);
    }


    public double calculateHeight() {
        double fieldCount = 0;
        double subPanelHeight = 0;
        for (Component c : getChildren().collect(Collectors.toList())) {
            if (c instanceof SubPanel) {
                subPanelHeight += ((SubPanel) c).calculateHeight();
            }
            else {
                fieldCount++;
            }
        }
        
        return Math.ceil(fieldCount / COLUMN_COUNT) * FIELD_HEIGHT
                + PADDING_HEIGHT + subPanelHeight;
    }
    
}
