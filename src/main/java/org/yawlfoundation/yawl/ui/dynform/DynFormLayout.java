package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private static final int SIMPLE_FIELD_LAYOUT_THRESHOLD = 3;
    private static final int BASE_WIDTH = 350;

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


    // if there are less than 4 fields, and any that are sub-panels have only one
    // simple field, and any not are also simple fields, set half width
    public String getAppropriateWidth() {
        return getAppropriateWidthAsInt() + "px";
    }


    private int getAppropriateWidthAsInt() {
        if (getChildren().count() <= SIMPLE_FIELD_LAYOUT_THRESHOLD) {
            for (SubPanel subPanel : getChildSubPanels(this)) {
                if (! isSimpleContentSubPanel(subPanel)) {
                    return BASE_WIDTH * 2;    // go wide for non-simple sub-panels
                }
            }
            return BASE_WIDTH + getMaxSubPanelDepth(this) * 30;
        }
        return BASE_WIDTH * 2;    // more fields than threshold
    }


    private int getMaxSubPanelDepth(Component parent) {
        int depth = 0;
        for (SubPanel subPanel : getChildSubPanels(parent)) {
             depth = Math.max(depth, 1 + getMaxSubPanelDepth(subPanel));
        }
        return depth;
    }

    public void setAppropriateHeight(int windowHeight) {
        double actualHeight = calculateHeight();
        double height = Math.min(windowHeight * WINDOW_HEIGHT_FRACTION, actualHeight);
        setHeight(height + "px");
    }


    private boolean isSimpleContentSubPanel(SubPanel subPanel) {
        DynFormLayout content = getSubPanelContent(subPanel);

        for (Component c : content.getChildren().collect(Collectors.toList())) {
             if (c instanceof SubPanel) {
                 if (! isSimpleContentSubPanel((SubPanel) c)) {
                     return false;
                 }
             }
        }
        return content.getChildren().filter(child -> !(child instanceof SubPanel)).count() < 2;
    }


    private List<SubPanel> getChildSubPanels(Component parent) {
        List<SubPanel> subPanels = new ArrayList<>();
        parent.getChildren().filter(component -> (component instanceof SubPanel))
                .forEach(p -> subPanels.add((SubPanel) p));
        return subPanels;
    }


    private DynFormLayout getSubPanelContent(SubPanel subPanel) {
        return (DynFormLayout) subPanel.getChildren().filter(
                child -> (child instanceof DynFormLayout)).findAny().orElse(null);
    }


    private void setColspan(Component component) {
        int colspan = (component instanceof SubPanel || component instanceof ChoiceComponent) ?
                COLUMN_COUNT : 1;
        super.setColspan(component, colspan);
    }


    public double calculateHeight() {
        double height = 0;
        
        // all simple fields < threshold
        if (isSimpleFieldsUnderThreshold()) {
            return calculateSimpleFieldsUnderThresholdHeight() + PADDING_HEIGHT;
        }

        // some simple sub-panels in mix, but still < threshold
        if (getAppropriateWidthAsInt() < BASE_WIDTH * 2) {
            for (Component c : getChildren().collect(Collectors.toList())) {
                if (c instanceof SubPanel) {
                    height += ((SubPanel) c).calculateHeight();
                }
                else if (c instanceof ChoiceComponent) {
                    height += ((ChoiceComponent) c).calculateHeight();
                }
                else {
                    height += getSimpleFieldHeight(c);     // checkboxes are shorter
                }
            }
            return height + PADDING_HEIGHT;
        }
        
        for (ImmutablePair<Component, Component> row : collectFormIntoRows()) {
            if (row.left instanceof SubPanel) {
                height += ((SubPanel) row.left).calculateHeight();
            }
            else if (row.left instanceof ChoiceComponent) {
                height += ((ChoiceComponent) row.left).calculateHeight();
            }
            else {
                height += getMaxRowHeight(row);           // checkboxes are shorter
            }
        }
        return height + PADDING_HEIGHT;
    }


    // a max of 4 'simple' fields (textfield, combo, checkbox, etc) in one column
    private boolean isSimpleFieldsUnderThreshold() {
        return getChildren().count() <= SIMPLE_FIELD_LAYOUT_THRESHOLD &&
                getMaxColSpan() < COLUMN_COUNT;
    }


    // all types of field have the same screen height, except for checkboxes
    private double calculateSimpleFieldsUnderThresholdHeight() {
        double height = 0;
        for (Component c : getChildren().collect(Collectors.toList())) {
            height += getSimpleFieldHeight(c);
        }
        return height;
    }


    private double getSimpleFieldHeight(Component c) {
        return (c instanceof Checkbox) ? CHECKBOX_HEIGHT : FIELD_HEIGHT;
    }


    // collects the form into rows of one (spanning two columns) or two (spanning one
    // column each) fields. For a row of (potentially) two fields, the second column
    // may have a field or may be blank.  
    private List<ImmutablePair<Component, Component>> collectFormIntoRows() {
        List<ImmutablePair<Component, Component>> rows = new ArrayList<>();
        Component left = null;
        for (Component c : getChildren().collect(Collectors.toList())) {

            // sub-panels & choice panels span both cols, so they fill one row.
            // if there's already something in the left, that's a row on its own
            // (blank on the right hand side)
             if (getColspan(c) == 2) {
                 if (left != null) {
                     rows.add(new ImmutablePair<>(left, null));
                 }
                 rows.add(new ImmutablePair<>(c, null));
                 left = null;
             }
             else {                      // single field
                 if (left != null) {
                     rows.add(new ImmutablePair<>(left, c));
                     left = null;
                 }
                 else {
                     left = c;
                 }
             }
        }
        if (left != null) {                                 // if last row, only a left
            rows.add(new ImmutablePair<>(left, null));
        }
        return rows;
    }


    private int getMaxColSpan() {
        for (Component c : getChildren().collect(Collectors.toList())) {
            if (getColspan(c) > 1) {
                return COLUMN_COUNT;
            }
        }
        return 1;
    }


    private double getMaxRowHeight(ImmutablePair<Component, Component> row) {
        double leftHeight = getFieldHeight(row.left);
        double rightHeight = row.right != null ? getFieldHeight(row.right) : 0;
        return Math.max(leftHeight, rightHeight);
    }


    private double getFieldHeight(Component c) {
        return c instanceof Checkbox ? CHECKBOX_HEIGHT : FIELD_HEIGHT;
    }
    
}
