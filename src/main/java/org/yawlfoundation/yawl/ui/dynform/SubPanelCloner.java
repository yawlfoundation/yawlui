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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.*;
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

            // clone panel content
            List<Component> content = cloneContent(panel);
            if (!content.isEmpty()) {
                newPanel.addContent(content);
            }
//        if (newPanel.getController().canVaryOccurs()) {
//            newPanel.addOccursButton(_factory.makeOccursButton(name, "+"));
//            newPanel.addOccursButton(_factory.makeOccursButton(name, "-"));
//            newPanel.getBtnPlus().setStyle(panel.getBtnPlus().getStyle());
//            newPanel.getBtnMinus().setStyle(panel.getBtnMinus().getStyle());
//        }
            return newPanel;
        }
        catch (CloneNotSupportedException e) {
            return new SubPanel(_factory);
        }
    }


    private List<Component> cloneContent(SubPanel panel) {
        List<Component> clonedContent = new ArrayList<>();
        panel.getChildren().forEach(child -> {
            if (child instanceof SubPanel) {
                clonedContent.add(cloneSubPanel((SubPanel) child));               // recurse
            }
            else {
                List<Component> newContent = cloneSimpleComponent(child);
                if (newContent != null) {
                    clonedContent.addAll(newContent);
                }
            }
        });
        return clonedContent;
    }


    private List<Component> cloneSimpleComponent(Component component) {
        List<Component> clonedComponents = new ArrayList<>();
        Component clonedComponent = null ;

//        else if (component instanceof RadioButton)
//            clonedComponent = cloneRadioButton(component);

        if (component instanceof TextField)
            clonedComponent = cloneTextField((TextField) component);
        else if (component instanceof DatePicker)
            clonedComponent = cloneDatePicker((DatePicker) component);
        else if (component instanceof Checkbox)
            clonedComponent = cloneCheckbox((Checkbox) component);
        else if (component instanceof ComboBox)
            clonedComponent = cloneComboBox((ComboBox<?>) component);
        else if (component instanceof Div)
            clonedComponent = cloneTextBlock((Div) component) ;

        if (clonedComponent != null) {
            clonedComponents.add(clonedComponent);
            return clonedComponents ;
        }
        else return null ;
    }


    public TextField cloneTextField(TextField oldField) {
        TextField clonedField = new TextField(oldField.getLabel()) ;
        clonedField.setValue(oldField.getValue());
        clonedField.setRequired(oldField.isRequired());
        clonedField.setEnabled(oldField.isEnabled());
        clonedField.setClassName(oldField.getClassName());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyle(oldField, clonedField);
        _factory.addClonedFieldToTable(oldField, clonedField);      // for later validation
        return clonedField;
    }


    public DatePicker cloneDatePicker(DatePicker oldField) {
        DatePicker clonedField = new DatePicker(oldField.getLabel());
        clonedField.setValue(oldField.getValue());
        clonedField.setEnabled(oldField.isEnabled());
        clonedField.setRequired(oldField.isRequired());
        clonedField.setMin(oldField.getMin());
        clonedField.setMax(oldField.getMax());
        clonedField.setClassName(oldField.getClassName());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyle(oldField, clonedField);
        return clonedField ;
    }


    public Checkbox cloneCheckbox(Checkbox oldField) {
        Checkbox clonedField = new Checkbox(oldField.getLabel()) ;
        clonedField.setValue(oldField.getValue()) ;
        clonedField.setEnabled(oldField.isEnabled());
        clonedField.setClassName(oldField.getClassName());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyle(oldField, clonedField);
        return clonedField ;
    }

    
    public <T> ComboBox<T> cloneComboBox(ComboBox<T> oldField) {
        ComboBox<T> clonedField = new ComboBox<>(oldField.getLabel()) ;
        clonedField.setItems(oldField.getListDataView().getItems().collect(Collectors.toList()));
        clonedField.setValue(oldField.getValue());
        clonedField.setClassName(oldField.getClassName());
        UiUtil.copyTooltip(oldField, clonedField);
        cloneStyle(oldField, clonedField);
        return clonedField ;
    }


//    public RadioButton cloneRadioButton(UIComponent field) {
//        RadioButton oldRadio = (RadioButton) field;
//        RadioButton newRadio = new RadioButton();
//        newRadio.setId(createUniqueID(oldRadio.getId()));
//        newRadio.setName(oldRadio.getName() + _rbGroupID);                // new rb group
//        newRadio.setSelected(oldRadio.getSelected());
//        newRadio.setStyle(oldRadio.getStyle());
//        newRadio.setStyleClass(oldRadio.getStyleClass());
//        return newRadio;

//    }


    public Div cloneTextBlock(Div oldField) {
        Div clonedField = new Div();
        clonedField.setText(oldField.getText());
        clonedField.setClassName(oldField.getClassName());
        cloneStyle(oldField, clonedField);
        return clonedField;
    }


    private void cloneStyle(HasStyle component, HasStyle cloned) {
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
                if (controller != null) {
                    controller = processed.get(name) ;
                }
                else {
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
