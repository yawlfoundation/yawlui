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

/**
 * This class is responsible for generating dynamic forms
 * Author: Michael Adams
 * Creation Date: 19/01/2008
 * Refactored 10/08/2008 - 04/2010 - 09/2014 - 10/2022
 */


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;
import org.jdom2.Element;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.jsf.dynform.FormParameter;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.dynform.dynattributes.DynAttributeFactory;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.util.JDOMUtil;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class DynFormFactory {

    // the set of generated subpanels on the current form
    private final Map<String, SubPanelController> _subPanelTable = new HashMap<>();

    // a map of inputs to the textfields they generated (required for validation)
    private Map<Component, DynFormField> _componentFieldTable;

    // the workitem's extended attributes (decomposition level) *
    private DynFormUserAttributes _userAttributes = new DynFormUserAttributes();

    // the object that manufactures the form's fields *
    private DynFormFieldAssembler _fieldAssembler;

    // the wir currently populating the form
    private WorkItemRecord _wir;

    // the top-level container
    private DynFormLayout _container;

    // a YAWL Engine API client
    private final EngineClient _engineClient;


    public DynFormFactory(EngineClient client) {
        _engineClient = client;
    }


    /**
     * Build a form to capture the work item's output data values from a user.
     *
     * @param schema An XSD schema of the data types and attributes to display
     * @param wir the work item record
     * @return the generated form
     */
    public DynFormLayout createForm(String schema, WorkItemRecord wir, Participant p)
            throws DynFormException {
        _wir = wir;
        _userAttributes.set(wir.getAttributeTable());
        return buildForm(schema, getWorkItemData(wir), getParamInfo(wir), p);
    }


    /**
     * Build a form to capture the input data values on a case start from a user.
     *
     * @param schema An XSD schema of the data types and attributes to display
     * @param parameters a list of the root net's input parameters
     * @return the generated form
     */
    public DynFormLayout createForm(String schema, List<YParameter> parameters, Participant p)
            throws DynFormException {
        return buildForm(schema, null, getCaseParamMap(parameters), p);
    }


    /**
     * Gets the form's data list on completion of the form. The data list will be
     * a well-formed XML string representing the expected data structure for the work
     * item output or case start. The opening and closing tag must be the name of task of
     * which the work item is an instance, or of the root net name of the case instance.
     *
     * @return A well-formed XML String of the form's data values
     */
    public String getDataList() {
        return new DataListGenerator(this).generate(_container,
                _fieldAssembler.getFieldList());
    }


    public List<Long> getDocComponentIDs() {
        List<Long> ids = new ArrayList<>();
        _container.getChildren().filter(child -> child instanceof DocComponent)
                .forEach(dc -> ids.add(((DocComponent) dc).getID()));
        return ids;
    }



    public boolean validate() {
        return new DynFormValidator().validate(_container, _componentFieldTable);
    }


    protected String getDefaultFormName() { return _fieldAssembler.getFormName(); }


    protected DynFormField getFieldForComponent(Component component) {
        return (component != null) ? _componentFieldTable.get(component) : null;
    }

    protected EngineClient getEngineClient() { return _engineClient; }

    /* @return the decomposition-level extended attributes */
    protected DynFormUserAttributes getUserAttributes() { return _userAttributes; }


    /* @return the data of the displayed workitem */
    protected String getWorkItemData() { return getWorkItemData(_wir); }


    protected void addSubPanelControllerMap(Map<String, SubPanelController> map) {
        for (SubPanelController controller : map.values()) {
            _subPanelTable.put(createUniqueID("clonedGroup"), controller);
        }
    }


    protected void addClonedFieldToTable(TextField orig, TextField clone) {
        DynFormField field = _componentFieldTable.get(orig);
        if (field != null) {
            _componentFieldTable.put(clone, field);
        }
    }


    protected Map<String, String> getUserDefinedFormFontStyles() {
        return _userAttributes.getUserDefinedFontStyles();
    }


    protected Map<String, String> getUserDefinedHeaderFontStyles() {
        return _userAttributes.getUserDefinedFontStyles(true);
    }


    protected String getCaseID() {
        return _wir != null ? _wir.getRootCaseID() : null;
    }


    protected String getFormName() {
        if (_wir != null) {
            String udLabel = getTaskLabel();
            return enspace(_wir.getCaseID() + ":" +
                    (udLabel != null ? udLabel : _fieldAssembler.getFormName()));
        }
        return _fieldAssembler.getFormName();
    }


    protected String enspace(String text) { return replaceInternalChars(text, '_', ' '); }

    protected String despace(String text) { return replaceInternalChars(text, ' ', '_'); }


    private DynFormLayout buildForm(String schema, String data,
                                    Map<String, FormParameter> paramMap,
                                    Participant participant) throws DynFormException {
        _fieldAssembler = new DynFormFieldAssembler(schema, data, paramMap);
        buildForm(participant);
        return _container;
    }


    private void buildForm(Participant participant) {
        setFormBackgroundColour();
        DynFormComponentBuilder builder = new DynFormComponentBuilder(this);
        List<DynFormField> fieldList = _fieldAssembler.getFieldList();
        DynAttributeFactory.adjustFields(fieldList, _wir, participant);   // 1st pass
        _container = new DynFormLayout(getDefaultFormName());
        _container.add(buildInnerForm(builder, fieldList));
        DynAttributeFactory.applyAttributes(_container, _wir, participant);  // 2nd pass
        _componentFieldTable = builder.getComponentFieldMap();
    }


    private DynFormComponentList buildInnerForm(DynFormComponentBuilder builder,
                                                List<DynFormField> fieldList) {
        DynFormComponentList componentList = new DynFormComponentList();
        if (fieldList == null) return componentList;

        for (DynFormField field : fieldList) {
            if (field.isFieldContainer()) {

                // new complex type - recurse in a new sub-container
                componentList.addAll(buildSubPanel(builder, field));
            }
            else if (!field.isEmptyOptionalInputOnly()) {  // create the field (inside a panel)

                // if min and/or max defined at the field level, enclose it in a subpanel
                if (field.isGroupedField()) {
                    componentList.addAll(buildSubPanel(builder, field));
                }
                else {
                    componentList.addAll(builder.makeInputField(field));
                }
            }
        }
        componentList.consolidateChoiceComponents(fieldList);
        return componentList;
    }

    
    /* @return a map of workitem parameters [param name, param] */
    private Map<String, FormParameter> getParamInfo(WorkItemRecord wir) {
        try {
            TaskInformation taskInfo = _engineClient.getTaskInformation(wir);
            return new ParameterMap().build(wir, taskInfo);
        }
        catch (IOException e) {
            return Collections.emptyMap();
        }
    }


    private Map<String, FormParameter> getCaseParamMap(List<YParameter> paramList) {
        return new ParameterMap().toMap(paramList);
    }


    private String getWorkItemData(WorkItemRecord wir) {
        if (wir == null) return null;
        Element data = wir.getUpdatedData() != null ? wir.getUpdatedData() :
                wir.getDataList();
        return JDOMUtil.elementToStringDump(data);
    }



    private void setFormBackgroundColour() {
        String udBgColour = getPanelBackgroundColour();
        if (udBgColour != null) {
            _container.getStyle().set("background-color", udBgColour);
        }
    }


    private DynFormComponentList buildSubPanel(DynFormComponentBuilder builder,
                                               DynFormField field) {
        DynFormComponentList compList = builder.makePeripheralComponents(field, true);
        SubPanelController spc = _subPanelTable.get(field.getGroupID());
        SubPanel subPanel = builder.makeSubPanel(field, spc);
        _subPanelTable.put(field.getGroupID(), subPanel.getController());
        DynFormComponentList innerContent;
        if (field.isFieldContainer()) {
            innerContent = buildInnerForm(builder, field.getSubFieldList());
        } else {
            innerContent = builder.makeInputField(field);
            field.addSubField(field.clone());
        }
        subPanel.addContent(innerContent);
        compList.add(subPanel);
        compList.addAll(builder.makePeripheralComponents(field, false));

        return compList;
    }

    
    private String createUniqueID(String id) { return IdGenerator.uniquify(id); }


     private void removeSubPanelController(SubPanel panel) {
        _subPanelTable.remove(panel.getName());
    }


    public void removeOrphanedControllers(SubPanel panel) {
        List<Component> subPanels = panel.getChildren().filter(
                child -> child instanceof SubPanel).collect(Collectors.toList());
        subPanels.forEach(orphan  -> {
            removeSubPanelController((SubPanel) orphan);
            removeOrphanedControllers((SubPanel) orphan);  // recurse
        });
    }


    // replaces each internally occurring 'pre' char with a 'post' char
    private String replaceInternalChars(String text, char pre, char post) {
        if ((text == null) || (text.length() < 3) || (text.indexOf(pre) < 0)) return text;

        char[] chars = text.toCharArray();

        // ignore leading and trailing underscores
        for (int i = 1; i < chars.length - 1; i++) {
            if (chars[i] == pre) chars[i] = post;
        }
        return new String(chars);
    }

    
    // support for decomposition extended attributes


    protected String getPanelBackgroundColour() {
        return getAttributeValue("background-color");
    }


    protected String getPanelAltBackgroundColour() {
        return getAttributeValue("background-alt-color");
    }


    private Map<String, String> getFormFontStyle() {
        return _userAttributes.getUserDefinedFontStyles();
    }


    private Font getFormFont() {
        return _userAttributes.getUserDefinedFont();
    }


    private Font getFormHeaderFont() {
        return _userAttributes.getFormHeaderFont();
    }


    public Map<String, String> getFormHeaderFontStyle() {
        return _userAttributes.getFormHeaderFontStyle();
    }


    protected String getFormJustify() {
        return _userAttributes.getTextJustify();
    }


    protected boolean isFormReadOnly() {
        return _userAttributes.isReadOnly();
    }


    private String getTaskLabel() {
        return getAttributeValue("label");
    }


    private String getAttributeValue(String key) {
        return key == null ? key : _userAttributes.getValue(key);
    }

}
