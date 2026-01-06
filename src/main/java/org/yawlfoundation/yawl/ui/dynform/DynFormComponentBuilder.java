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
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.resourcing.jsf.FontUtil;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Michael Adams
 * Creation Date: 10/08/2008
 */
public class DynFormComponentBuilder {

    // for setting focus on first available component
    private final DynFormFactory _factory ;
    private DynTextParser _textParser;
    private final Map<Component, DynFormField> _componentFieldTable;


    public DynFormComponentBuilder(DynFormFactory factory) {
        _factory = factory;
        _componentFieldTable = new HashMap<>();
    }


    public SubPanel makeSubPanel(DynFormField field, SubPanelController controller) {
        SubPanel subPanel = new SubPanel(_factory, field);
        if ((! field.isChoiceField()) && field.isFieldContainer()) {
            subPanel.addHeader(makeHeaderText(field.getLabel(), field.getName()));
        }
        if (controller != null) {
            controller.storeSubPanel(subPanel);
        }
        else {
            controller = new SubPanelController(subPanel, field.getMinoccurs(),
                                         field.getMaxoccurs(), field.getLevel(),
                                         _factory.getPanelBackgroundColour(),
                                         _factory.getPanelAltBackgroundColour()) ;
            subPanel.setController(controller);
        }
        if (controller.canVaryOccurs()) {
            subPanel.addOccursButtons();

            if (isReadOnly(field)) {
                subPanel.enableOccursButtons(false);
            }
            else {
                controller.setOccursButtonsEnablement();
            }    
        }
        subPanel.setVisible(! field.isHidden(_factory.getWorkItemData()));
        return subPanel ;
    }


    public DynFormComponentList makeInputField(DynFormField input) {
        Component field;
        DynFormComponentList fieldList = makePeripheralComponents(input, true);
        
        String type = input.getDataTypeUnprefixed();
        if (input.isEmptyComplexTypeFlag() || type.equals("boolean")) {
            field = makeCheckbox(input);
        }
        else {
            if (type.equals("date")) {
                field = makeDatePicker(input);
            }
            else if (type.equals("dateTime")) {
                field = makeDateTimePicker(input);
            }
            else if (type.equals("YDocumentType")) {
                field = makeDocumentField(input);
            }
            else if (input.hasEnumeratedValues()) {
                field = makeEnumeratedList(input);
            }
            else if (input.isTextArea()) {
                field = makeTextArea(input);
            }
            else field = makeTextField(input);
        }
        fieldList.add(field);

        _componentFieldTable.put(field, input);            // store for validation later

        fieldList.addAll(makePeripheralComponents(input, false));
        return fieldList ;
    }


    public DynFormComponentList makePeripheralComponents(DynFormField input, boolean above) {
        DynFormComponentList list = new DynFormComponentList();
        boolean makeLine = above ? input.isLineAbove() : input.isLineBelow() ;
        if (makeLine) {
            list.add(makeHLine());
        }
        String text = above ? input.getTextAbove() : input.getTextBelow();
        if (text != null) {
            list.add(makeTextBlock(input, text));
        }
        String imagePath = above ? input.getImageAbove() : input.getImageBelow();
        if (imagePath != null) {
            Image image = new Image(imagePath, "");
            String align = above ? input.getImageAboveAlign() : input.getImageBelowAlign();
            if (align != null) {
                image.getStyle().set("align", align);
            }
            list.add(image);
        }
        return list;
    }


    private Map<String, String> makeStyleMap(Component field, DynFormField input) {
        Map<String, String> styles = new HashMap<>(mergeFontStyles(
                input.getUserDefinedFontStyle()));

        // set justify - precedence is variable, form, none
        if (field instanceof TextField || field instanceof TextArea || field instanceof ComboBox) {
            String justify = input.getTextJustify();

            // if not set at variable level, user global form level (if defined)
            if (justify == null) {
                justify = _factory.getFormJustify();    
            }
            if (justify != null) {
                styles.put("text-align", justify);
            }
        }

        if (input.hasBlackoutAttribute()) {
            styles.put("background-color", "black");
        }
        else {
            String bgColour = input.getBackgroundColour();
            if (bgColour != null) {
                styles.put("background-color", bgColour);
            }
        }

        return styles;
    }


    // merge UD-form fonts with UD-field fonts (field takes precedence)
    private Map<String, String> mergeFontStyles(Map<String, String> fieldStyles) {
        Map<String, String> formStyles = _factory.getUserDefinedFormFontStyles();
        for (String styleKey : formStyles.keySet()) {
             if (!fieldStyles.containsKey(styleKey)) {
                 fieldStyles.put(styleKey, formStyles.get(styleKey));
             }
        }
        return fieldStyles;
    }


    public H5 makeHeaderText(String text, String defText) {
        String headerText = text != null ? text : _factory.enspace(defText);
        H5 header = new H5(headerText);
        setStyles(header, _factory.getUserDefinedHeaderFontStyles());
        return header;
    }


    public Checkbox makeCheckbox(DynFormField input) {
        Checkbox checkbox = new Checkbox(input.getLabel());
        checkbox.setValue((input.getValue() != null) &&
                          input.getValue().equalsIgnoreCase("true")) ;
        checkbox.setReadOnly(isReadOnly(input));
        setStyles(checkbox, input);
        checkbox.getStyle().set("margin-top", "10px");
        checkbox.setVisible(isVisible(input));
        UiUtil.setTooltip(checkbox, input.getToolTip());
        return checkbox;
    }


    public DatePicker makeDatePicker(DynFormField input) {
        DatePicker datePicker = new DatePicker(input.getLabel());
        datePicker.setValue(createDate(input.getValue(), -1));       // default to today
        datePicker.setReadOnly(isReadOnly(input));
        datePicker.setMin(getMinDate(input));
        datePicker.setMax(getMaxDate(input));
        setStyles(datePicker, input);
        datePicker.setVisible(isVisible(input));
        UiUtil.setTooltip(datePicker, input.getToolTip());
        return datePicker;
    }


    public DateTimePicker makeDateTimePicker(DynFormField input) {
        DateTimePicker dateTimePicker = new DateTimePicker(input.getLabel());
        dateTimePicker.setValue(createDateTime(input.getValue(), -1));       // default to today
        dateTimePicker.setReadOnly(isReadOnly(input));
        dateTimePicker.setMin(getMinDateTime(input));
        dateTimePicker.setMax(getMaxDateTime(input));
        setStyles(dateTimePicker, input);
        dateTimePicker.setVisible(isVisible(input));
        UiUtil.setTooltip(dateTimePicker, input.getToolTip());
        return dateTimePicker;
    }


    private void setStyles(Component component, DynFormField input) {
        setStyles(component, makeStyleMap(component, input));
    }


    private void setStyles(Component component, Map<String, String> styles) {
        for (Map.Entry<String, String> entry : styles.entrySet()) {
            UiUtil.setStyle(component, entry.getKey(), entry.getValue());
        }
    }


    private LocalDate getMinDate(DynFormField input) {
        LocalDate minDate = LocalDate.MIN;
        DynFormFieldRestriction restriction = input.getRestriction();
        if (restriction != null) {
            if (restriction.hasMinInclusive()) {
                minDate = createDate(restriction.getMinInclusive(), 1);   // def. 1/1/70
            }
            else if (restriction.hasMinExclusive()) {
                minDate = createDate(restriction.getMinExclusive(), 1)
                        .plusDays(1);
            }
        }
        return minDate;
    }

    private LocalDateTime getMinDateTime(DynFormField input) {
        LocalDateTime minDateTime = LocalDateTime.MIN;
        DynFormFieldRestriction restriction = input.getRestriction();
        if (restriction != null) {
            if (restriction.hasMinInclusive()) {
                minDateTime = createDateTime(restriction.getMinInclusive(), 1);   // def. 1/1/70
            }
            else if (restriction.hasMinExclusive()) {
                minDateTime = createDateTime(restriction.getMinExclusive(), 1)
                        .plusSeconds(1);
            }
        }
        return minDateTime;
    }


    private LocalDate getMaxDate(DynFormField input) {
        LocalDate maxDate = LocalDate.MAX;
        DynFormFieldRestriction restriction = input.getRestriction();
        if (restriction != null) {
            if (restriction.hasMaxInclusive()) {
                maxDate = createDate(restriction.getMaxInclusive(), Long.MAX_VALUE);
            }
            else if (restriction.hasMaxExclusive()) {
                maxDate = createDate(restriction.getMaxExclusive(), Long.MAX_VALUE)
                        .minusDays(1);
            }
        }
        return maxDate;
    }

    private LocalDateTime getMaxDateTime(DynFormField input) {
        LocalDateTime maxDateTime = LocalDateTime.MAX;
        DynFormFieldRestriction restriction = input.getRestriction();
        if (restriction != null) {
            if (restriction.hasMaxInclusive()) {
                maxDateTime = createDateTime(restriction.getMaxInclusive(), Long.MAX_VALUE);
            }
            else if (restriction.hasMaxExclusive()) {
                maxDateTime = createDateTime(restriction.getMaxExclusive(), Long.MAX_VALUE)
                        .minusSeconds(1);
            }
        }
        return maxDateTime;
    }


    public ComboBox<String> makeEnumeratedList(DynFormField input) {
        ComboBox<String> comboBox = new ComboBox<>(input.getLabel());
        setStyles(comboBox, input);
        List<String> items = input.getEnumeratedValues();
        comboBox.setItems(items);
        if (! StringUtil.isNullOrEmpty(input.getValue())) {
            comboBox.setValue(input.getValue());
        }
        else if (! items.isEmpty()) {
            comboBox.setValue(items.get(0));
        }
        comboBox.setReadOnly(isReadOnly(input));
        comboBox.setVisible(isVisible(input));
        UiUtil.setTooltip(comboBox, input.getToolTip());
        return comboBox;
    }

    
    public TextArea makeTextArea(DynFormField input) {
        TextArea textarea = new TextArea(input.getLabel());
        setStyles(textarea, input);
        textarea.setReadOnly(isReadOnly(input));
        UiUtil.setTooltip(textarea, input.getToolTip());
        textarea.setVisible(isVisible(input));
        if (! (input.hasBlackoutAttribute() || input.getValue() == null)) {
            textarea.setValue(JDOMUtil.decodeEscapes(input.getValue()));
        }
        input.setRestrictionAttributes();
        return textarea;

    }

    public TextField makeTextField(DynFormField input) {
        TextField textField = new TextField(input.getLabel()) ;
        setStyles(textField, input);
        textField.setReadOnly(isReadOnly(input));
        UiUtil.setTooltip(textField, input.getToolTip());
        textField.setVisible(isVisible(input));
        if (! (input.hasBlackoutAttribute() || input.getValue() == null)) {
            textField.setValue(JDOMUtil.decodeEscapes(input.getValue()));
        }
        input.setRestrictionAttributes();
        return textField ;
    }

    
    public DocComponent makeDocumentField(DynFormField input) {
        DynFormField name = input.getSubField("name");
        DynFormField id = input.getSubField("id");
        TextField textField = makeTextField(name);
        boolean inputOnly = input.isInputOnly();
        return new DocComponent(_factory.getCaseID(), id.getValue(), textField, inputOnly);
    }


    private Div makeTextBlock(DynFormField input, String text) {
        Div block = new Div();
        block.setText(parseText(text));
        setStyles(block, input.getUserDefinedFontStyle());
        return block; 
    }


    private Hr makeHLine() {
        Hr hr = new Hr();
        setStyles(hr, Map.of("background-color", "blue","flex", "0 0 2px",
                "align-self", "stretch"));
        return hr;
    }


    private Image makeImageComponent(String imagePath) {
        return new Image(imagePath, "");
    }



    private boolean isVisible(DynFormField input) {
        return ! input.isHidden(_factory.getWorkItemData());
    }


    private boolean isReadOnly(DynFormField input) {
        return input.isInputOnly() || _factory.isFormReadOnly();
    }


    private String parseText(String text) {
        if (_textParser == null)
            _textParser = new DynTextParser(_factory.getWorkItemData());
        return _textParser.parse(text);
    }


    private LocalDate createDate(String dateStr, long defaultDate) {
        // set the date to the param's input value if possible, else default to a
        // date representation of the long defaultDate, or today if defaultDate < 0

        if (dateStr != null) {
            try {
                return LocalDate.parse(dateStr);
            }
            catch (DateTimeParseException pe) {
                if (defaultDate > -1) {
                    return Instant.ofEpochMilli(defaultDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                }
            }
        }
        return LocalDate.now();
    }


    private LocalDateTime createDateTime(String dateTimeStr, long defaultDate) {
        // set the date to the param's input value if possible, else default to a
        // date representation of the long defaultDate, or today if defaultDate < 0

        if (dateTimeStr != null) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            }
            catch (DateTimeParseException pe) {
                if (defaultDate > -1) {
                    return Instant.ofEpochMilli(defaultDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }
        }
        return LocalDateTime.now();
    }


    public Map<Component, DynFormField> getComponentFieldMap() {
        return _componentFieldTable;
    }


    private int getTextWidth(DynFormField input, String text) {

        // use field's udFont first, then form's, then default
        Font font = input.getFont();
        if (font == null) font = _factory.getUserAttributes().getUserDefinedFont();
        return FontUtil.getTextWidth(text, font);
    }

}
