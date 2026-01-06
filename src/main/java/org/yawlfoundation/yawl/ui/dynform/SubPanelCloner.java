/*
 * Copyright (c) 2004-2020 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: Michael Adams
 * Creation Date: 7/08/2008
 */
public class SubPanelCloner {

    private DynFormFactory _factory ;

    public SubPanelCloner() { }

    public SubPanel clone(SubPanel panel, DynFormFactory factory) {
        _factory = factory;
        SubPanel cloned = cloneSubPanel(panel);
        resolveChildControllers(cloned);
        return cloned;
    }


    private SubPanel cloneSubPanel(SubPanel panel)  {
        try {
            SubPanel newPanel = panel.clone();
            newPanel.addHeader(cloneHeader(panel));

            // clone panel content
            List<Component> content = cloneContent(panel);
            if (!content.isEmpty()) {
                newPanel.addContent(content);
            }
            if (newPanel.getController().canVaryOccurs()) {
                newPanel.addOccursButtons();
                newPanel.getController().setOccursButtonsEnablement();
            }
            return newPanel;
        }
        catch (CloneNotSupportedException e) {
            return new SubPanel(_factory, panel.getField());
        }
    }


    private List<Component> cloneContent(SubPanel panel) {
        List<Component> clonedContent = new ArrayList<>();
        panel.getForm().getChildren().forEach(child -> {
            Component cloned = cloneComponent(child);
            if (cloned != null) {
                clonedContent.add(cloned);
            }
        });
        return clonedContent;
    }


    private Component cloneComponent(Component component) {
        if (component instanceof SubPanel) {
            return cloneSubPanel((SubPanel) component);               // recurse
        }
        else {
            return cloneSimpleComponent(component);
        }
    }

    
    private Component cloneSimpleComponent(Component component) {
        if (component instanceof TextField) {
            return cloneTextField((TextField) component);
        }
        if (component instanceof DatePicker) {
            return cloneDatePicker((DatePicker) component);
        }
        if (component instanceof DateTimePicker) {
            return cloneDateTimePicker((DateTimePicker) component);
        }
        if (component instanceof Checkbox) {
            return cloneCheckbox((Checkbox) component);
        }
        if (component instanceof ComboBox) {
            return cloneComboBox((ComboBox<?>) component);
        }
        if (component instanceof ChoiceComponent) {
            return cloneChoiceComponent((ChoiceComponent) component);
        }
        if (component instanceof Div) {
            return cloneTextBlock((Div) component);
        }

        return null;
    }


    public TextField cloneTextField(TextField oldField) {
        TextField clonedField = new TextField(oldField.getLabel()) ;
//        clonedField.setValue(oldField.getValue());
        clonedField.setRequired(oldField.isRequired());
        clonedField.setEnabled(oldField.isEnabled());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyles(oldField, clonedField);
        _factory.addClonedFieldToTable(oldField, clonedField);      // for later validation
        return clonedField;
    }


    public DatePicker cloneDatePicker(DatePicker oldField) {
        DatePicker clonedField = new DatePicker(oldField.getLabel());
 //       clonedField.setValue(oldField.getValue());
        clonedField.setRequired(oldField.isRequired());
        clonedField.setEnabled(oldField.isEnabled());
        clonedField.setMin(oldField.getMin());
        clonedField.setMax(oldField.getMax());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyles(oldField, clonedField);
        return clonedField;
    }


    public DateTimePicker cloneDateTimePicker(DateTimePicker oldField) {
        DateTimePicker clonedField = new DateTimePicker(oldField.getLabel());
 //       clonedField.setValue(oldField.getValue());
        clonedField.setEnabled(oldField.isEnabled());
        clonedField.setMin(oldField.getMin());
        clonedField.setMax(oldField.getMax());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyles(oldField, clonedField);
        return clonedField;
    }


    public Checkbox cloneCheckbox(Checkbox oldField) {
        Checkbox clonedField = new Checkbox(oldField.getLabel()) ;
 //       clonedField.setValue(oldField.getValue()) ;
        clonedField.setEnabled(oldField.isEnabled());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyles(oldField, clonedField);
        return clonedField ;
    }

    
    public <T> ComboBox<T> cloneComboBox(ComboBox<T> oldField) {
        ComboBox<T> clonedField = new ComboBox<>(oldField.getLabel()) ;
        clonedField.setItems(oldField.getListDataView().getItems().collect(Collectors.toList()));
 //       clonedField.setValue(oldField.getValue());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyles(oldField, clonedField);
        return clonedField ;
    }


    public ChoiceComponent cloneChoiceComponent(ChoiceComponent oldComponent) {
        List<Component> clonedContent = new ArrayList<>();
        for (Component component : oldComponent.getContent()) {
            Component cloned = cloneComponent(component);
            if (cloned != null) {
                clonedContent.add(cloned);
            }
        }
        return new ChoiceComponent(clonedContent, null);
    }


    public Div cloneTextBlock(Div oldField) {
        Div clonedField = new Div();
 //       clonedField.setText(oldField.getText());
        cloneStyles(oldField, clonedField);
        return clonedField;
    }


    public H5 cloneHeader(SubPanel panel) {
        H5 oldHeader = panel.getHeader();
        if (oldHeader != null) {
            H5 clonedHeader = new H5(oldHeader.getText());
            cloneStyles(oldHeader, clonedHeader);
            return clonedHeader;
        }
        return null;
    }


    private void cloneStyles(HasStyle component, HasStyle cloned) {
        component.getStyle().getNames().forEach(name ->
                cloned.getStyle().set(name, component.getStyle().get(name)));
    }


    private void resolveChildControllers(SubPanel panel) {
        Map<String, SubPanelController> processed = new HashMap<>();

        panel.getChildren().forEach(component -> {
            if (component instanceof SubPanel) {          // child panel
                SubPanel childPanel = (SubPanel) component;
                String name = childPanel.getName();
                SubPanelController controller = processed.get(name);
                if (controller == null) {
                    try {
                        controller = childPanel.getController().clone();
                        processed.put(name, controller);
                    }
                    catch (CloneNotSupportedException e) {
                        // won't happen;
                    }
                }
                if (controller != null) {
                    controller.storeSubPanel(childPanel);
                    if (controller.canVaryOccurs())
                        controller.setOccursButtonsEnablement();
                    childPanel.setController(controller);
                    resolveChildControllers(childPanel);                   // recurse
                }
            }
        });

        if (! processed.isEmpty()) _factory.addSubPanelControllerMap(processed);
    }

}
