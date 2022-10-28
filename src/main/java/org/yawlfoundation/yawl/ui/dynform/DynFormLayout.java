package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 13/10/2022
 */
public class DynFormLayout extends FormLayout {

    public static final double FIELD_HEIGHT = 64.5;
    public static final double CHECKBOX_HEIGHT = 22.8;
    private static final double PADDING_HEIGHT = 31.0;
    private static final double WINDOW_HEIGHT_FRACTION = 0.65;
    private static final int COLUMN_COUNT = 2;

    private final String _name;

    public DynFormLayout(String name) {
        super();
        _name = name;
    }


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


    public String getName() { return _name; }


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
        double actualHeight = calculateHeight() - PADDING_HEIGHT;
        double height = Math.min(windowHeight * WINDOW_HEIGHT_FRACTION, actualHeight);
        setHeight(height + "px");
    }

    
    private void setColspan(Component component) {
        int colspan = (component instanceof SubPanel || component instanceof ChoiceComponent) ?
                COLUMN_COUNT : 1;
        super.setColspan(component, colspan);
    }


    public double calculateHeight() {
        double fieldCount = 0;
        double checkboxCount = 0;
        double subPanelHeight = 0;
        for (Component c : getChildren().collect(Collectors.toList())) {
            if (c instanceof SubPanel) {
                subPanelHeight += ((SubPanel) c).calculateHeight();
            }
            else if (c instanceof ChoiceComponent) {
                subPanelHeight += ((ChoiceComponent) c).calculateHeight();
            }
            else if (c instanceof Checkbox) {
                checkboxCount++;
            }
            else {
                fieldCount++;
            }
        }
        
        return Math.ceil(fieldCount / COLUMN_COUNT) * FIELD_HEIGHT
                + Math.ceil(checkboxCount / COLUMN_COUNT) * CHECKBOX_HEIGHT
                + PADDING_HEIGHT + subPanelHeight;
    }
    
}
