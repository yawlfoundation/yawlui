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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: Michael Adams
 * Creation Date: 7/08/2008
 */
public class DataListGenerator {

    private final DynFormFactory _factory;

    public DataListGenerator(DynFormFactory factory) {
        _factory = factory;
    }

    
    public String generate(DynFormLayout panel, List<DynFormField> fieldList) {
        return generateDataList(panel, fieldList) ;
    }

    
    private String generateDataList(Component panel, List<DynFormField> fieldList) {
        StringBuilder result = new StringBuilder() ;
        List<Component> children = panel.getChildren().collect(Collectors.toList());
        String parentTag = "";
        int start = 1;              // by default ignore first child (static text header)
        int stop = children.size();                 // by default process all components

        // a simpletype choice outer container has a radio button as first child
        Object o = children.get(0);
//        if (o instanceof RadioButton) {
//            SelectedChoiceBounds.calcBounds(children);
//            start = SelectedChoiceBounds.start;
//            stop = SelectedChoiceBounds.stop;
//        }
//        else {

            // a subpanel child of a choice also has no header
//            if (! headedPanel(panel)) {
//                start = 0;
//            }
//            else {
                // if this is the outermost panel, its title may be user defined, so get
                // the default form name
                if (! (panel instanceof SubPanel)) {
                    parentTag = _factory.getDefaultFormName();
                }
                else {
                    // otherwise get the default name of the panel (and thus the element name)
                    parentTag = _factory.despace(((SubPanel) panel).getName()) ;
                }
                result.append("<").append(parentTag).append(">") ;
  //          }
  //      }

        for (int i = start; i < stop; i++) {
            Component child = children.get(i) ;

            // if subpanel, build inner output recursively
            if (child instanceof SubPanel) {
                DynFormField field = getField(child, fieldList);
                String dataList = generateDataList((SubPanel) child, field.getSubFieldList());

                // don't add empty elements for field with minOccurs=0
                if (field.hasZeroMinimum() && StringUtil.unwrap(dataList).isEmpty()) {
                    continue;
                }

                result.append(dataList);
            }

            // if a complextype choice, then forward start and stop to correct posns
//            else if (child instanceof RadioButton) {
//                SelectedChoiceBounds.calcBounds(children);
//                i = SelectedChoiceBounds.start - 1;   // will readd +1 in next loop
//                stop = SelectedChoiceBounds.stop;
//            }

            // each label is a reference to an input field
            else {// if (child instanceof Label) {
                DynFormField field = getField(child, fieldList);
                result.append(getFieldValue(child, field)) ;
            }
        }

        // close the xml and return
        if (parentTag.length() > 0) result.append("</").append(parentTag).append(">") ;
        return result.toString();
    }


    private String getFieldValue(Component component, DynFormField field) {
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
        else if (component instanceof ComboBox) {
            value = String.valueOf(((ComboBox<?>) component).getValue());
        }
        else if (component instanceof TextArea) {
            value = JDOMUtil.encodeEscapes(((TextArea) component).getValue());
        }

        return formatField(value, field);
    }
    

    private String formatField(String value, DynFormField field) {

        // if no value & minOccurs=0 then don't output anything
        if (((value == null) || (value.length() == 0)) && field.hasZeroMinimum()) {
            return "";
        }

        // special case (1) - optional enum list, no selection
        if ((value != null) && value.equals("<-- Choose (optional) -->")) {
            return "";
        }

        // special case (2) - empty complex type flag definition
        if (field.isEmptyComplexTypeFlag()) {
            if ((value == null) || value.equals("false")) {
                return "";                         // no data element for this field
            }
            else value = "";                       // empty data element for this field
        }

        if (field.hasBlackoutAttribute()) value = field.getValue();

        return StringUtil.wrap(value, field.getName());
    }


    private boolean headedPanel(SubPanel panel) {
        Optional<Component> header = panel.getChildren().filter(
                component -> component instanceof H4).findAny();
        return header.isPresent();
    }


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
            try {
                DynFormField formField = _factory.getFieldForComponent(component);
                if (formField != null) {
                    return formField;
                }
            }
            catch (ClassCastException cce) {
                // fall through to code that follows...
            }

            // fallback to label text for identification (may be ambiguous if two
            // fields have the same label)
//            id = (String) component.get;
//            id = id.replaceAll(":", "").trim();
//            for (DynFormField field : fieldList) {
//                if (field.getLabel().equals(id))
//                    return field;
//            }
        }
        return null;
    }


    /******************************************************************************/

    /**
     * Calculates and holds the bounds of the selected field(s) within a choice panel 
     */
    static class SelectedChoiceBounds {
        static int start = -1;
        static int stop = -1;

        /**
         * Works through a choice panel's components, finding the selected radio button
         * then marking the bounds of it and the last component of the selection.
         * @param children the panel's child components
         */
        static void calcBounds(List children) {
            start = -1;
            stop = -1;
            int i = 0;
            do {
                Object o = children.get(i);
//                if (o instanceof RadioButton) {
//                    RadioButton rb = (RadioButton) o;
//                    if (rb.isChecked()) {           // found the selected radio
//                        start = i + 1;              // so start at next component
//                    }
//                    else {                          // else if radio unselected
//                        if (start > -1) {           // and selected one previously found
//                            stop = i ;              // then end one before next radio
//                        }
//                    }
//                }
                i++;

            } while ((stop == -1) && (i < children.size()));

            // if no later radio found, upper bound is last component in the list
            if (stop == -1) stop = children.size();
        }

    }

}
