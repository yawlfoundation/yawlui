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
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.xml.sax.SAXException;
import org.yawlfoundation.yawl.schema.ErrorHandler;
import org.yawlfoundation.yawl.util.DOMUtil;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Michael Adams
 * Creation Date: 7/08/2008
 */

public class DynFormValidator {

    private List<String> _messages;
    private Map<Component, DynFormField> _componentFieldLookup;

    public final static String NS_URI = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    public final static String NS_PREFIX = "xsd";

    private final String _ns = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private final SchemaFactory _factory = SchemaFactory.newInstance(_ns);
    private final ErrorHandler _errorHandler = new ErrorHandler();


    public DynFormValidator() {
        _factory.setErrorHandler(_errorHandler);
    }

    
    public boolean validate(DynFormLayout panel,
                            Map<Component, DynFormField> componentFieldLookup) {

        _componentFieldLookup = componentFieldLookup;
        _messages = new ArrayList<>();

        return validateInputs(panel);
    }


    private boolean validateInputs(DynFormLayout panel) {
        AtomicBoolean finalResult = new AtomicBoolean(true);

        // checkboxes & dropdowns are self validating - only need to do textfields
        // & calendars (calendars only for non-empty required values)
        panel.getChildren().forEach(component -> {

            boolean subResult = true;
            if (component instanceof SubPanel) {
                subResult = validateInputs(((SubPanel) component).getForm());
            }
            else if (component instanceof ChoiceComponent) {
                subResult = validateInputs(
                        ((ChoiceComponent) component).getChosenPanel().getForm());
            }
            else if (component instanceof TextField) {
                subResult = validateTextField((TextField) component);
            }
            else if (component instanceof TextArea) {
                subResult = validateTextArea((TextArea) component);
            }

            // need to check that if its required it has a value
            else if ((component instanceof DatePicker)) {
                subResult = validateDatePicker((DatePicker) component);
            }
            else if ((component instanceof DateTimePicker)) {
                subResult = validateDateTimePicker((DateTimePicker) component);
            }

            finalResult.set(finalResult.get() && subResult);

        });

        return finalResult.get();
    }


    private boolean validateTextField(TextField field) {
        String text = field.getValue();
        DynFormField input = _componentFieldLookup.get(field);
        if (input != null) {
            if (! input.hasSkipValidationAttribute()) {
                return isTimerExpiryField(input) ? validateExpiry(field) :
                        validateField(field, input, text);
            }
        }
        return true;
    }


    private boolean validateTextArea(TextArea field) {
        String text = field.getValue();
        DynFormField input = _componentFieldLookup.get(field);
        if (input != null) {
            if (! input.hasSkipValidationAttribute()) {
                return validateField(field, input, text);
            }
        }
        return true;
    }


    private boolean validateField(HasValidation field, DynFormField input, String value) {
        field.setInvalid(! validateRequired(field, input, value));
        if (! (field.isInvalid() || StringUtil.isNullOrEmpty(value))) {
            String msg = validateAgainstSchema(input, value);
            if (msg != null) {
                field.setErrorMessage(msg);
                field.setInvalid(true);
            }
        }
        return ! field.isInvalid();
    }


    private boolean validateDatePicker(DatePicker field) {
        field.setInvalid(false);
        DynFormField input = _componentFieldLookup.get(field);
        if ((input != null) && (! input.hasSkipValidationAttribute())) {
            if (input.isRequired() && field.getValue() == null) {
                field.setErrorMessage("A date is required");
                field.setInvalid(true);
            }
        }
        return ! field.isInvalid();
    }


    private boolean validateDateTimePicker(DateTimePicker field) {
        field.setInvalid(false);
        DynFormField input = _componentFieldLookup.get(field);
        if ((input != null) && (! input.hasSkipValidationAttribute())) {
            if (input.isRequired() && field.getValue() == null) {
                field.setErrorMessage("A date time is required");
                field.setInvalid(true);
            }
        }
        return ! field.isInvalid();
    }



    private boolean validateExpiry(TextField field) {
        field.setInvalid(false);
        String value = field.getValue();
        try {
            DatatypeFactory.newInstance().newDuration(value);    // try duration 1st
            return true;
        }
        catch (Exception e) {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                sdf.parse(value);
                return true;
            }
            catch (ParseException pe) {
                field.setErrorMessage("A valid Duration or DateTime string is required");
                field.setInvalid(true);
                return false;
            }
        }
    }


    private boolean validateBase(String fieldName, String type, String text,
                                 boolean untreated) {
         boolean result = true;

         if ((text != null) && (text.length() > 0)) {
             if (type.equals("string"))
                 result = true ;
             else if (type.equals("long"))
                 result = validateLong(text, fieldName, untreated);
             else if (type.equals("int"))
                 result = validateInt(text, fieldName, untreated);                 
             else if (type.equals("double"))
                 result = validateDouble(text, fieldName, untreated);
             else if (type.equals("decimal"))
                 result = validateDecimal(text, fieldName, untreated);
             else if (type.equals("time"))
                 result = validateTime(text, fieldName, untreated);
             else if (type.equals("duration"))
                 result = validateDuration(text, fieldName, untreated);
         }

         return result ;
     }


    private boolean validateRequired(HasValidation field, DynFormField input, String value) {
        field.setInvalid(false);
        if (input.isRequired() && StringUtil.isNullOrEmpty(value)) {
            field.setErrorMessage("A value is required");
            field.setInvalid(true);
        }
        return ! field.isInvalid();
    }


     private boolean validateLong(String value, String fieldName, boolean untreated) {
         try {
             new Long(value);
             return true;
         }
         catch (NumberFormatException nfe) {
             addValidationErrorMessage(value, fieldName, "long", untreated) ;
             return false ;
         }
     }


    private boolean validateInt(String value, String fieldName, boolean untreated) {
        try {
            new Integer(value);
            return true;
        }
        catch (NumberFormatException nfe) {
            addValidationErrorMessage(value, fieldName, "integer", untreated) ;
            return false ;
        }
    }


     private boolean validateDouble(String value, String fieldName, boolean untreated) {
         try {
             new Double(value);
             return true;
         }
         catch (NumberFormatException nfe) {
             addValidationErrorMessage(value, fieldName, "double", untreated) ;
             return false ;
         }
     }


    private boolean validateDecimal(String value, String fieldName, boolean untreated) {
         try {
             new Double(value);
             return true;
         }
         catch (NumberFormatException nfe) {
             addValidationErrorMessage(value, fieldName, "decimal", untreated) ;
             return false ;
         }
     }


     private boolean validateTime(String value, String fieldName, boolean untreated) {
         try {
             DateFormat df = DateFormat.getTimeInstance();
             df.parse(value);
             return true;
         }
         catch (ParseException pe) {
             addValidationErrorMessage(value, fieldName, "time", untreated) ;
             return false ;
         }
     }


     private boolean validateDuration(String value, String fieldName, boolean untreated) {
         try {
             DatatypeFactory factory = DatatypeFactory.newInstance();
             factory.newDuration(value);
             return true;
         }
         catch (Exception e) {
             addValidationErrorMessage(value, fieldName, "duration", untreated) ;
             return false ;
         }
     }



    private void addRestrictionErrorMessage(String type, DynFormField input) {
        String msg = null;
        String value = "";
        String name = input.getLabel();
        if (type.equals("minLength")) {
            value = input.getRestriction().getMinLength();
            msg = "Field '%s' requires a value of at least %s characters.";
        }
        else if (type.equals("maxLength")) {
            value = input.getRestriction().getMaxLength();
            msg = "Field '%s' can have a value of no more than %s characters.";
        }
        else if (type.equals("length")) {
            value = input.getRestriction().getLength();
            msg = "Field '%s' requires a value of exactly %s characters.";
        }
        else if (type.equals("pattern")) {
            value = input.getRestriction().getPattern();
            msg = "The value in field '%s' must match the pattern '%s'.";
        }
        else if (type.equals("minInclusive")) {
            value = input.getRestriction().getMinInclusive();
            msg = "The value in field '%s' cannot be less than %s.";
        }
        else if (type.equals("minExclusive")) {
            value = input.getRestriction().getMinExclusive();
            msg = "The value in field '%s' must be greater than %s.";
        }
        else if (type.equals("maxInclusive")) {
            value = input.getRestriction().getMaxInclusive();
            msg = "The value in field '%s' cannot be greater than %s.";
        }
        else if (type.equals("maxExclusive")) {
            value = input.getRestriction().getMaxExclusive();
            msg = "The value in field '%s' must be less than %s.";
        }
        else if (type.equals("totalDigits")) {
            value = input.getRestriction().getTotalDigits();
            msg = "The value in field '%s' must have exactly %s digits.";
        }
        else if (type.equals("fractionDigits")) {
            value = input.getRestriction().getFractionDigits();
            msg = "The value in field '%s' must have exactly %s digits after the decimal point.";
        }
        if (! (msg == null || _messages == null)) {
            _messages.add(String.format(msg, name, value));
        }
    }


    private String formatSchemaErrorMessage(DynFormField input) {
        String msg = input.getAlertText();
        if (msg == null) {
            msg = String.format("A valid value of %s type is required",
                    input.getDataTypeUnprefixed());
            if (input.hasRestriction()) {
                msg += input.getRestriction().getToolTipExtn();
            }
        }
        return msg;
    }

    
     private void addValidationErrorMessage(String value, String fieldName,
                                            String type, boolean untreated) {
         String name = untreated ? normaliseFieldName(fieldName) : fieldName ;
         String msg  =
             String.format("Invalid value '%s' in field '%s', expecting a valid %s value",
                            value, name, type);
         _messages.add(msg);
     }


     private String normaliseFieldName(String name) {
         char[] chars = name.toCharArray();
         int len = chars.length ;
         char c = chars[--len];
         while ((c >= '0') && (c <= '9')) c = chars[--len];
         char[] result = new char[len-2];
         for (int i = 3; i <= len; i++) {
             result[i-3] = chars[i];
         }
         return new String(result);
     }

     

    private boolean isEmptyValue(String value) {
        return ((value == null) || (value.length() < 1));
    }


    private boolean isTimerExpiryField(DynFormField input) {
        return input.getParam().getDataTypeName().equals("YTimerType");
    }


    private String validateAgainstSchema(DynFormField input, String value) {
        try {
            _errorHandler.reset();
            Schema schema = _factory.newSchema(getInputSchema(input));
            if (_errorHandler.isValid()) {
                Validator validator = schema.newValidator();
                validator.setErrorHandler(_errorHandler);
                validator.validate(getInputValueAsXML(input, value));
                if (! _errorHandler.isValid()) {
                    return formatSchemaErrorMessage(input);
                }
            }
        }
        catch (SAXException | IOException e) {
            return e.getMessage();
        }

        return null;
    }


    private SAXSource getInputSchema(DynFormField input) {
        return createSAXSource(buildInputSchema(input));
    }


    private SAXSource getInputValueAsXML(DynFormField input, String value) {
        if (input.getDataTypeUnprefixed().equals("string"))
            value = JDOMUtil.encodeEscapes(value);               // encode string values
        return createSAXSource(StringUtil.wrap(value, input.getName()));
    }


    private SAXSource createSAXSource(String xml) {
        SAXSource result = null;
        try {
            result = new SAXSource(DOMUtil.createUTF8InputSource(xml));
        }
        catch(UnsupportedEncodingException uee) {
            // nothing to do - null will be returned
        }
        return result;
    }


    private String buildInputSchema(DynFormField input) {
        StringBuilder schema = new StringBuilder(getSchemaHeader());

        schema.append("<")
              .append(NS_PREFIX)
              .append(":element name=\"")
              .append(input.getName())
              .append("\"");

        if (input.hasUnion()) {
            schema.append(">")
                  .append(input.getUnion().getBaseElement())
                  .append("</xsd:element>");
        }
        else if (input.hasRestriction()) {
            schema.append(">")
                  .append(input.getRestriction().getBaseElement())
                  .append("</xsd:element>");
        }
        else {
            schema.append(" type=\"xsd:")
                  .append(input.getDataTypeUnprefixed())
                  .append("\"/>");
        }
        schema.append("</xsd:schema>") ;

        String schemaString = schema.toString();
        schemaString = schemaString.replaceAll("xs:", NS_PREFIX + ":");
        schemaString = schemaString.replaceAll("yawl:", NS_PREFIX + ":");
        
        return schemaString;
    }


    private String getSchemaHeader() {
        return String.format("<%s:schema xmlns:%s=\"%s\">", NS_PREFIX, NS_PREFIX, NS_URI); 
    }

}
