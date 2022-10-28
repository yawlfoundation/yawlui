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
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.XNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds an XML data string from the contents of a DynForm, representing the output
 * document of a work item or the input parameters of a case start
 * Author: Michael Adams
 * Creation Date: 7/08/2008
 * Last Date: 28/10/2022 (for v.5)
 */
public class DataListGenerator {

    private final DynFormFactory _factory;

    public DataListGenerator(DynFormFactory factory) {
        _factory = factory;
    }


    /**
     * Generate a data document from a DynForm
     * @param container the outermost container of the form
     * @param fieldList the corresponding list of hierarchical field definitions
     * @return An XML data document (as a string)
     */
    public String generate(DynFormLayout container, List<DynFormField> fieldList) {
        return generateDataList(container, fieldList).toString();
    }


    /**
     * Recursively build a data document from the contents of a DynForm
     * @param container A form container (either a SubPanel or a DynFormLayout component)
     * @param fieldList the corresponding list of hierarchical field definitions
     * @return An XML data document (as a string) for this container
     */
    private XNode generateDataList(Component container, List<DynFormField> fieldList) {
        XNode node = createNode(container);

        // get all child fields, ignoring H5 titles
        List<Component> children = container.getChildren()
                .filter(c -> ! (c instanceof H5)).collect(Collectors.toList());

        for (Component child : children) {

            // if child is choice, replace child with actual chosen panel (a SubPanel)
            if (child instanceof ChoiceComponent) {
                child = ((ChoiceComponent) child).getChosenPanel();
            }

            // get corresponding field info for the child component
            DynFormField field = getField(child, fieldList);

            // if subpanel, build inner content recursively
            if (child instanceof SubPanel) {
                XNode subNode = generateDataList(((SubPanel) child).getForm(),
                        field.getSubFieldList());
                addContent(node, subNode, field);
            }
            else {

                // simple field
                XNode fieldNode = createFieldNode(child, field);
                if (fieldNode != null) node.addChild(fieldNode);
            }
        }

        return node;
    }

    /* Create a new xml node for the container. The name will match the element name */
    private XNode createNode(Component component) {
        String name;
        if (component instanceof SubPanel) {
            name = _factory.despace(((SubPanel) component).getName());
        }
        else if (component instanceof DynFormLayout) {
            name = _factory.despace(((DynFormLayout) component).getName());
        }
        else {
            name = _factory.getDefaultFormName();
        }
        return new XNode(name);
    }


    /* Insert the inner content xml */
    private void addContent(XNode parent, XNode child, DynFormField field) {

        // don't add empty elements for field with minOccurs=0
        if (! (field.hasZeroMinimum() && ! child.hasChildren())) {
            parent.addChild(child);
        }
    }


    /* Extract the value from a UI component */
    private XNode createFieldNode(Component component, DynFormField field) {
        String value = null;
        if (component instanceof DocComponent) {
            value = ((DocComponent) component).getOutputXML();
        }
        else if (component instanceof TextField) {
            value = JDOMUtil.encodeEscapes(((TextField) component).getValue());
        }
        else if (component instanceof Checkbox) {
            value = String.valueOf(((Checkbox) component).getValue());
        }
        else if (component instanceof DatePicker) {
            LocalDate date = ((DatePicker) component).getValue();
            value = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        else if (component instanceof DateTimePicker) {
            LocalDateTime dateTime = ((DateTimePicker) component).getValue();
            value = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        else if (component instanceof ComboBox) {
            value = String.valueOf(((ComboBox<?>) component).getValue());
        }
        else if (component instanceof TextArea) {
            value = JDOMUtil.encodeEscapes(((TextArea) component).getValue());
        }

        return formatField(field, value);
    }
    

    /* After checks, build a correctly formed node for a single element */
    private XNode formatField(DynFormField field, String value) {

        // if no value & minOccurs=0 then don't output anything
        if (StringUtil.isNullOrEmpty(value) && field.hasZeroMinimum()) {
            return null;
        }

        // special case (1) - optional enum list, no selection
        if ((value != null) && value.equals("<-- Choose (optional) -->")) {
            return null;
        }

        // special case (2) - empty complex type flag definition
        if (field.isEmptyComplexTypeFlag()) {
            if ((value == null) || value.equals("false")) {
                return null;                         // no data element for this field
            }
            else value = "";                       // empty data element for this field
        }

        if (field.hasBlackoutAttribute()) {
            value = field.getValue();              // default input as output
        }

        XNode node = new XNode(field.getName());
        if (field.isYDocument()) {
            node.addContent(value);                // a YDoc has two elements to add
        }
        else {
            node.setText(value);
        }
        return node;
    }

    /* Extract the corresponding field info for a component */
    private DynFormField getField(Component component, List<DynFormField> fieldList) {
        if (component instanceof SubPanel) {
            String name = ((SubPanel) component).getName();
            for (DynFormField field : fieldList) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }
        else {

            // first try to get field directly
            DynFormField field = _factory.getFieldForComponent(component);
            if (field != null) {
                return field;
            }
            
            // fallback to label text for identification (may be ambiguous if two
            // fields have the same label)
            String label = ((HasLabel) component).getLabel();
            for (DynFormField field1 : fieldList) {
                if (field1.getLabel().equals(label)) {
                     return field1;
                 }
            }
        }
        return null;            // should never be reached
    }
    
}
