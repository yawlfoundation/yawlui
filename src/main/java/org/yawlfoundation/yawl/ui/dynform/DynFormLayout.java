package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.yawlfoundation.yawl.schema.internal.YInternalTypeUtil;
import org.yawlfoundation.yawl.ui.listener.DynFormContentChangeListener;
import org.yawlfoundation.yawl.ui.listener.DynFormContentChangedEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 13/10/2022
 */
public class DynFormLayout extends FormLayout {

    public static final double FIELD_HEIGHT = 64.5;
    public static final double CHECKBOX_HEIGHT = 22.8;
    public static final double HR_HEIGHT = 25.0;

    private static final double PADDING_HEIGHT = 31.0;
    private static final double WINDOW_HEIGHT_FRACTION = 0.65;
    private static final int COLUMN_COUNT = 2;
    private static final int SIMPLE_FIELD_LAYOUT_THRESHOLD = 3;
    private static final int BASE_WIDTH = 350;

    private final Set<DynFormContentChangeListener> _listeners = new HashSet<>();
    private final String _name;
    private final String _id = UUID.randomUUID().toString();
    private String _varName;
    private String _dataType;
    private String _impliedDataType;
    private DynFormField _input;


    // called from DynFormFactory to create outer container
    public DynFormLayout(String name) {
        super();
        _name = name;
    }


    // called from SubPanel to create container panel for complex type
    public DynFormLayout(DynFormField field) {
        this(field.getName());
        _dataType = field.getParam().getDataTypeName();
        _impliedDataType = field.getImpliedDataType();
        if (field.hasParent()) {
            _varName = field.getParent().getName();
        }
    }


    public void addContentChangeListener(DynFormContentChangeListener listener) {
        _listeners.add(listener);
    }


    @Override
    public void add(Collection<Component> components) {
        super.add(components);
        components.forEach(this::setColspan);
    }


    @Override
    public void addComponentAtIndex(int index, Component component) {
        if (index < getComponentCount()) {
            super.addComponentAtIndex(index, component);
        }
        else {
            super.add(component);
        }
        setColspan(component);
    }


    @Override
    public void addComponentAsFirst(Component component) {
        super.addComponentAsFirst(component);
        setColspan(component);
   }

    @Override
    public void remove(Component... components) {
        super.remove(components);
        for (Component component : components) {
            announceRemovedComponent(component);
        }
    }


    public String getID() { return _id; }
    

    public String getName() { return _name; }


    public DynFormLayout getParentLayout() {
        return getParentLayout(this);
    }


    public DynFormLayout getParentLayout(DynFormLayout child) {
        Optional<Component> parentLayout = child.getParent().flatMap(Component::getParent);
        return (DynFormLayout) parentLayout.orElse(null);
    }


    // if there are less than 4 fields, and any that are sub-panels have only one
    // simple field, and any not are also simple fields, set half width
    public String getAppropriateWidth() {
        return getAppropriateWidthAsInt() + "px";
    }


    public void applyInternalTypeReference() {
        if (isGeoDataType()) {
            collectTextFields().forEach(this::addListenersForGeoTypes);
        }

        for (SubPanel subPanel : getChildSubPanels()) {
            subPanel.getForm().applyInternalTypeReference();
        }
    }


    public void setInternalTypeReference(String type) { _impliedDataType = type; }

    public String getInternalTypeReference() {
        return _impliedDataType;
    }

    public String getImpliedDataType() { return _impliedDataType; }
    
    public boolean hasGeoTypeInTree() {
        if (isGeoDataType()) return true;

        for (SubPanel subPanel : getChildSubPanels()) {
            DynFormLayout child = subPanel.getForm();
            if (child.hasGeoTypeInTree()) return true;
        }
        return false;
    }


    // if hide or hideIf attribute applied, don't use map to show component
    // returns true if some component in the tree is visible and uses a geo map
    public boolean containsVisibleGeoMapComponentInTree() {
        if (isGeoDataType() && isVisible()) return true;
        for (SubPanel subPanel : getChildSubPanels()) {
             DynFormLayout child = subPanel.getForm();
             if (child.containsVisibleGeoMapComponentInTree()) return true;
        }
        return false;
    }


    public void addGeoTypeChangeListenerToTree(DynFormContentChangeListener listener) {
        if (hasGeoTypeInTree()) {
            _listeners.add(listener);
        }
        for (SubPanel subPanel : getChildSubPanels(this)) {
            subPanel.getForm().addGeoTypeChangeListenerToTree(listener);
        }
    }


    public void addGeoTypeListener(SubPanel addedSubPanel) {
        if (hasGeoTypeInTree()) {
            _listeners.forEach(listener -> {
                    addedSubPanel.getForm().addGeoTypeChangeListenerToTree(listener);
                    listener.formContentChanged(
                            new DynFormContentChangedEvent(_id, addedSubPanel,
                                    DynFormContentChangedEvent.EventType.SUB_PANEL_ADDED));
            });
        }
    }

    
    public int getComponentCount() {
        return Math.toIntExact(getChildren().count());
    }


    private int getAppropriateWidthAsInt() {
        if (isGeoDataType()) {
            return BASE_WIDTH + getMaxSubPanelDepth(this) * 30;
        }
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


    public String getDataType() { return _dataType; }

    public String getDerivedDataType() {
        return _impliedDataType != null ? _impliedDataType : _dataType;
    }

    
    public boolean isGeoDataType() {
        return YInternalTypeUtil.isGeoTypeName(_dataType) ||
                YInternalTypeUtil.isGeoTypeName(_impliedDataType);
    }


    public boolean isGeoDataListType() {
        return YInternalTypeUtil.isGeoListTypeName(_dataType) ||
                YInternalTypeUtil.isGeoListTypeName(_impliedDataType);
    }


    public void addColorIndicator(String color) {
        Icon indicator = new Icon(VaadinIcon.CIRCLE);
        indicator.setSize("12px");
        indicator.getStyle().set("margin-left", "4px");
        indicator.setColor(color);
        addComponentAsFirst(new HorizontalLayout(indicator));
    }


    private boolean isGeoReferencedType() {
        return _impliedDataType != null;
    }


    private boolean isSimpleContentSubPanel(SubPanel subPanel) {
        DynFormLayout content = getSubPanelContent(subPanel);

        for (Component c : content.getChildren().toList()) {
             if (c instanceof SubPanel) {
                 if (! isSimpleContentSubPanel((SubPanel) c)) {
                     return false;
                 }
             }
        }
        return content.getChildren().filter(child -> !(child instanceof SubPanel)).count() < 2;
    }


    public List<SubPanel> getChildSubPanels() { return getChildSubPanels(this); }

    
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
        int colspan = (component instanceof SubPanel ||
                component instanceof ChoiceComponent ||
                component instanceof Hr ||
                component instanceof Div) ?
                COLUMN_COUNT : 1;
        super.setColspan(component, colspan);
    }


    public double calculateHeight() {
        AtomicReference<Double> height = new AtomicReference<>((double) 0);
        
        // all simple fields < threshold
        if (isSimpleFieldsUnderThreshold()) {
            return calculateSimpleFieldsUnderThresholdHeight() + PADDING_HEIGHT;
        }

        // some simple sub-panels in mix, but still < threshold
        if (getAppropriateWidthAsInt() < BASE_WIDTH * 2) {
            for (Component c : getChildren().toList()) {
                if (c instanceof SubPanel subPanel) {
                    height.updateAndGet(v -> v + subPanel.calculateHeight());
                }
                else if (c instanceof ChoiceComponent choice) {
                    height.updateAndGet(v -> v + choice.calculateHeight());
                }
                else {
                    height.updateAndGet(v -> v + getSimpleFieldHeight(c));  // checkboxes are shorter
                }
            }
            return height.get() + PADDING_HEIGHT;
        }
        
        for (ImmutablePair<Component, Component> row : collectFormIntoRows()) {
            if (row.left instanceof SubPanel subPanel) {
                height.updateAndGet(v -> v + subPanel.calculateHeight());
            }
            else if (row.left instanceof ChoiceComponent choice) {
                height.updateAndGet(v -> v + choice.calculateHeight());
            }
            else if (row.left instanceof Hr) {
                height.updateAndGet(v -> v + HR_HEIGHT);
            }
            else if (row.left instanceof Div textDiv) {
                textDiv.getElement().executeJs("return $0.offsetHeight;", textDiv)
                    .then(Integer.class, h -> {
                        System.out.println("The calculated height of the Div is: " + h + "px");
                        height.updateAndGet(v -> v + h);
                    });
            }
            else {
                height.updateAndGet(v -> v + getMaxRowHeight(row));   // checkboxes are shorter
            }
        }
        return height.get() + PADDING_HEIGHT;
    }


    // a max of 4 'simple' fields (textfield, combo, checkbox, etc) in one column
    private boolean isSimpleFieldsUnderThreshold() {
        if (getChildren().anyMatch(component -> component instanceof SubPanel)) {
            return false;
        }
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
        for (Component c : getChildren().toList()) {

            // don't include hidden fields
            if (! c.isVisible()) {
                continue;
            }

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
        if (isGeoDataType()) {
            return 1;
        }
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


    protected void addListenersForGeoTypes(TextField textfield) {
        if (isGeoDataType()) {
            textfield.addValueChangeListener(e -> {
                String fieldName = e.getSource().getLabel();
                String oldValue = e.getOldValue();
                String newValue = e.getValue();
                DynFormLayout parent = getParentLayout();
                String parentId = parent != null ? parent.getID() : null;
                DynFormContentChangedEvent event = new DynFormContentChangedEvent(
                        _id, parentId, _varName, _name, fieldName,
                        _dataType, oldValue, newValue);

                _listeners.forEach(l ->
                        l.formContentChanged(event));
            });
        }
    }

    
    private void announceRemovedComponent(Component c) {
        if (hasGeoTypeInTree()) {
            if (c instanceof SubPanel) {
                DynFormContentChangedEvent event = new DynFormContentChangedEvent(_id,
                        (SubPanel) c, DynFormContentChangedEvent.EventType.SUB_PANEL_REMOVED);
                _listeners.forEach(l ->
                        l.formContentChanged(event));
            }
        }
    }

    protected List<TextField> collectTextFields() {
        return collectTextFields(this);
    }

    private List<TextField> collectTextFields(DynFormLayout layout) {
        List<TextField> fieldList = new ArrayList<>();
        for (Component c : layout.getChildren().toList()) {
            if (c instanceof SubPanel) {
                fieldList.addAll(collectTextFields(((SubPanel) c).getForm()));
            }
            else if (c instanceof TextField) {
                fieldList.add((TextField) c);
            }
        }
        return fieldList;
    }

}
