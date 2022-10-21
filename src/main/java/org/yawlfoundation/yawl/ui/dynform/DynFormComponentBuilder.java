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
import java.util.Map;

/**
 * Author: Michael Adams
 * Creation Date: 10/08/2008
 */
public class DynFormComponentBuilder {

    // for setting focus on first available component
    private DynFormFactory _factory ;
    private DynTextParser _textParser;
    private final Map<Component, DynFormField> _componentFieldTable;

    private int _maxDropDownWidth = 0;
    private int _maxLabelWidth = 0;
    private int _maxTextValueWidth = 0;
    private int _maxImageWidth = 0;
    private boolean _hasCheckboxOnly = true;

    private final int DROPDOWN_BUTTON_WIDTH = 15;


    public DynFormComponentBuilder(DynFormFactory factory) {
        _factory = factory;
        _componentFieldTable = new HashMap<>();
    }


    public SubPanel makeSubPanel(DynFormField field, SubPanelController spc) {
        String name = field.getName();
        SubPanel subPanel = new SubPanel(_factory);
        subPanel.setName(name);
        subPanel.getStyle().set("border", "1px solid lightgray");
        if ((! name.startsWith("choice")) && field.isFieldContainer())
            subPanel.addHeader(makeHeaderText(field.getLabel(), name)) ;

        if (spc != null) {
            spc.storeSubPanel(subPanel);
        }
        else {
            spc = new SubPanelController(subPanel, field.getMinoccurs(),
                                         field.getMaxoccurs(), field.getLevel(),
                                         _factory.getFormBackgroundColour(),
                                         _factory.getFormAltBackgroundColour()) ;
            subPanel.setController(spc);
        }
        if (spc.canVaryOccurs()) {
            subPanel.addOccursButtons();

            if (isDisabled(field)) {
                subPanel.enableOccursButtons(false);
            }
            else {
                spc.setOccursButtonsEnablement();
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
            _hasCheckboxOnly = false;
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
        header.setClassName("dynFormPanelHeader");
        setStyles(header, _factory.getUserDefinedHeaderFontStyles());
        return header;
    }


    public Checkbox makeCheckbox(DynFormField input) {
        Checkbox checkbox = new Checkbox(input.getLabel());
        checkbox.setValue((input.getValue() != null) &&
                          input.getValue().equalsIgnoreCase("true")) ;
        checkbox.setEnabled(! isDisabled(input));
        checkbox.setClassName("dynformInput");
        setStyles(checkbox, input);
        checkbox.getStyle().set("margin-top", "10px");
        checkbox.setVisible(isVisible(input));
        UiUtil.setTooltip(checkbox, input.getToolTip());
        return checkbox;
    }


    public DatePicker makeDatePicker(DynFormField input) {
        DatePicker datePicker = new DatePicker(input.getLabel());
        datePicker.setValue(createDate(input.getValue(), -1));       // default to today
        datePicker.setEnabled(!isDisabled(input));
        datePicker.setMin(getMinDate(input));
        datePicker.setMax(getMaxDate(input));
        datePicker.setClassName(getInputStyleClass(input));
        setStyles(datePicker, input);
        datePicker.setVisible(isVisible(input));
        UiUtil.setTooltip(datePicker, input.getToolTip());
        return datePicker;
    }


    public DateTimePicker makeDateTimePicker(DynFormField input) {
        DateTimePicker dateTimePicker = new DateTimePicker(input.getLabel());
        dateTimePicker.setValue(createDateTime(input.getValue(), -1));       // default to today
        dateTimePicker.setEnabled(!isDisabled(input));
        dateTimePicker.setMin(getMinDateTime(input));
        dateTimePicker.setMax(getMaxDateTime(input));
        dateTimePicker.setClassName(getInputStyleClass(input));
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
        comboBox.setClassName(getInputStyleClass(input));
        setStyles(comboBox, input);
        comboBox.setItems(input.getEnumeratedValues());
        if (! StringUtil.isNullOrEmpty(input.getValue())) {
            comboBox.setValue(input.getValue());
        }
        comboBox.setEnabled(! isDisabled(input));
        comboBox.setVisible(isVisible(input));
        UiUtil.setTooltip(comboBox, input.getToolTip());
        return comboBox;
    }

    
    public TextArea makeTextArea(DynFormField input) {
        TextArea textarea = new TextArea();
        textarea.setClassName(getInputStyleClass(input));
        setStyles(textarea, input);
        textarea.setEnabled(! isDisabled(input));
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
        textField.setClassName(getInputStyleClass(input));
        setStyles(textField, input);
        textField.setEnabled(! isDisabled(input));
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
        DocComponent docField = new DocComponent(_factory.getEngineClient(),
                _factory.getCaseID(), id.getValue(), textField, inputOnly);
        docField.setClassName("dynformDocComponent");
        return docField;
    }


//    public RadioButton makeRadioButton(DynFormField input) {
//        RadioButton rb = new RadioButton();
//        rb.setId(createUniqueID("rb" + input.getName()));
//        rb.setLabel("");
//        rb.setName(input.getChoiceID());               // same name means same rb group
//        rb.setStyle(makeStyle(rb, input));
//        rb.setDisabled(isDisabled(input));
//        rb.setStyleClass("dynformRadioButton");
//        rb.setVisible(isVisible(input));
//        return rb;
//    }


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


    private boolean isDisabled(DynFormField input) {
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


    private String getInputStyleClass(DynFormField input) {
        if (isDisabled(input)) {
            return "dynformInputReadOnly";
        }
        else if (input.isRequired()) {
            return "dynformInputRequired";
        }
        else {                                         // read-write and not required
            return "dynformInput";
        }    
    }

    private void setMaxLabelWidth(DynFormField input) {
        _maxLabelWidth = Math.max(_maxLabelWidth, getTextWidth(input,
                input.getLabel() + ":"));
    }

    public int getMaxLabelWidth() {
        return _maxLabelWidth;
    }


    private void setMaxDropDownWidth(DynFormField input, String text) {
        _maxDropDownWidth = Math.max(_maxDropDownWidth, getTextWidth(input, text));
    }

    public int getMaxDropDownWidth() {
        return _maxDropDownWidth + DROPDOWN_BUTTON_WIDTH;
    }


    private void setMaxTextValueWidth(DynFormField input, String text, int buffer) {
        _maxTextValueWidth = Math.max(_maxTextValueWidth, getTextWidth(input, text) + buffer);
    }

    public int getMaxTextValueWidth() {
        return _maxTextValueWidth;
    }

    public void setMaxImageWidth(int width) {
        _maxImageWidth = Math.max(_maxImageWidth, width);
    }

    public int getMaxImageWidth() {
        return _maxImageWidth;
    }

    public boolean hasOnlyCheckboxes() {
        return _hasCheckboxOnly ;
    }

    public int getMaxFieldWidth() {
        return Math.max(getMaxDropDownWidth(), getMaxTextValueWidth());
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
