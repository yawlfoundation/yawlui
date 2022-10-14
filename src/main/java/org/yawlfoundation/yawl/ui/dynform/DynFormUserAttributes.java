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


import net.sf.saxon.s9api.SaxonApiException;
import org.jdom2.Document;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.SaxonUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages the set of user-defined (at design-time) extended attributes
 * of a workitem, some of which will affect the way the dynamic form is displayed.
 *
 * Author: Michael Adams
 */
public class DynFormUserAttributes {

    private Map<String, String> _attributeMap ;


    public DynFormUserAttributes() {
        _attributeMap = new HashMap<>();
    }

    public DynFormUserAttributes(Map<String, String> attributeMap) {
        _attributeMap = attributeMap ;
    }


    public void set(Map<String, String> attributeMap) {
        _attributeMap = attributeMap ;
    }


    public void merge(Map<String, String> mergeMap) {
        if (mergeMap != null) _attributeMap.putAll(mergeMap);
    }


    public String getValue(String attribute) {
        return _attributeMap.get(attribute);
    }


    public boolean hasValue(String attribute) {
        return getValue(attribute) != null;
    }


    public boolean getBooleanValue(String attribute) {
        String value = getValue(attribute);
        return (value != null) && value.equalsIgnoreCase("true");
    }


    public int getIntegerValue(String attribute) {
        return StringUtil.strToInt(getValue(attribute), -1);
    }

    // *** the standard attributes ***//

    public boolean isReadOnly() {
        return getBooleanValue("readOnly");
    }


    public boolean isHidden() {
        return getBooleanValue("hide");
    }


    public boolean isSkipValidation() {
        return getBooleanValue("skipValidation");
    }


    public boolean isBlackout() {
        return getBooleanValue("blackout");
    }

    public boolean isMandatory() {
        return getBooleanValue("mandatory");
    }

    public boolean isOptional() {
        return getBooleanValue("optional");
    }

    public boolean hasHideIfQuery() {
        return (hasValue("hideIf"));
    }

    public boolean isHideIf(String data) {
        String query = getValue("hideIf");
        if (query != null) {
            try {
                Document dataDoc = JDOMUtil.stringToDocument(data);
                String queryResult = SaxonUtil.evaluateQuery(query, dataDoc);
                return queryResult.equalsIgnoreCase("true");
            }
            catch (SaxonApiException saxonEx) {
                // nothing to do, will default to false
            }
        }
        return false;
    }


    public String getAlertText() {
        return getValue("alert");                    // a validation error message
    }


    public String getLabelText() {
        return getValue("label");
    }


    public String getToolTipText() {
        return getValue("tooltip");
    }

    public boolean isTextArea() {
        return getBooleanValue("textarea");
    }


    public String getTextJustify() {
        String justify = getValue("justify");
        if (justify != null && List.of("center", "right", "left").contains(justify)) {
            return justify;
        }
        return null;
    }


    public String getBackgroundColour() {
        return getValue("background-color");         
    }


    public Map<String, String> getUserDefinedFontStyles() {
        return getUserDefinedFontStyles(false);
    }


    public Font getUserDefinedFont() {
        return getUserDefinedFont(false);
    }


    public Map<String, String> getUserDefinedFontStyles(boolean header) {
        Map<String, String> styles = new HashMap<>();
        String head = header ? "header-" : "";
        String fontColour = getValue(head + "font-color");
        if ((fontColour != null) && (! isBlackout())) {
            styles.put("color", fontColour);
        }
        String fontFamily = getValue(head + "font-family");
        if (fontFamily != null) {
            styles.put("font-family", fontFamily);
        }
        String fontSize = getValue(head + "font-size");
        if (fontSize != null) {
            styles.put("font-size", fontSize);
        }
        String fontStyle = getValue(head + "font-style");
        if (fontStyle != null) {
            if (fontStyle.contains("bold")) {
                styles.put("font-weight", "bold");
            }
            if (fontStyle.contains("italic")) {
                styles.put("font-style", "italic");
            }
        }
        return styles;
    }


    public Font getUserDefinedFont(boolean header) {
        if (! hasFontAttributes(header)) return null;

        String head = header ? "header-" : "";
        String fontFamily = getValue(head + "font-family");
        String family = (fontFamily != null)  ?  fontFamily : "Helvetica";

        int fontSize = getIntegerValue(head + "font-size");
        int size = (fontSize > -1) ? fontSize : (header ? 18 : 12) ;

        int style = Font.PLAIN;
        String fontStyle = getValue(head + "font-style");
        if (fontStyle != null) {
            if (fontStyle.contains("bold") && fontStyle.contains("italic"))
                style = Font.BOLD | Font.ITALIC;
            else if (fontStyle.contains("bold"))
                style = Font.BOLD;
            else if (fontStyle.contains("italic"))
                style = Font.ITALIC;
        }
        return new Font(family, style, size);
    }


    public Map<String, String> getFormHeaderFontStyle() {
        return getUserDefinedFontStyles(true);
    }


    public Font getFormHeaderFont() {
        return getUserDefinedFont(true);
    }


    public String getImageAbove() {
        return getValue("image-above");
    }

    public String getImageBelow() {
        return getValue("image-below");
    }

    public String getImageAboveAlign() {
        return getValue("image-above-align");
    }

    public String getImageBelowAlign() {
        return getValue("image-below-align");
    }

    public boolean isLineAbove() {
        return getBooleanValue("line-above");
    }

    public boolean isLineBelow() {
        return getBooleanValue("line-below");
    }

    public String getTextAbove() {
        return getValue("text-above");
    }

    public String getTextBelow() {
        return getValue("text-below");
    }

    public int getMaxFieldWidth() { return getIntegerValue("max-field-width"); }


    private boolean hasFontAttributes(boolean header) {
        String head = header ? "header-" : "";
        return ! ((getValue(head + "font-family") == null) &&
                  (getValue(head + "font-size") == null) &&
                  (getValue(head + "font-style") == null));
    }
}
