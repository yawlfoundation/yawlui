package org.yawlfoundation.yawl.ui.dynform;

import org.jdom2.Element;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.TaskInformation;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.YParametersSchema;
import org.yawlfoundation.yawl.resourcing.jsf.dynform.FormParameter;
import org.yawlfoundation.yawl.util.JDOMUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 13/10/2022
 */
public class ParameterMap {


    public ParameterMap() {}

    public Map<String, FormParameter> build(WorkItemRecord wir, TaskInformation taskInfo) {
        YParametersSchema schema = taskInfo.getParamSchema();
        Map<String, FormParameter> inputs = toMap(schema.getInputParams());
        Map<String, FormParameter> outputs = toMap(schema.getOutputParams());

        // if param is only in input list, mark it as input-only
        for (String name : inputs.keySet()) {
            if (!outputs.containsKey(name)) {
                inputs.get(name).setInputOnly(true);
            }
        }

        // combine
        outputs.putAll(inputs);

        // now map data values to params
        Element itemData = wir.isEdited() ? wir.getUpdatedData() :
                JDOMUtil.stringToElement(wir.getDataListString());

        for (String name : outputs.keySet()) {
            Element data = itemData.getChild(name);
            if (data != null) {
                if (data.getContentSize() > 0)         // complex type
                    outputs.get(name).setValue(JDOMUtil.elementToStringDump(data));
                else                                   // simple type
                    outputs.get(name).setValue(itemData.getText());
            }
        }
        return outputs;
    }


    public Map<String, FormParameter> toMap(List<YParameter> params) {
        Map<String, FormParameter> map = new HashMap<>();
        if (params != null) {
            for (YParameter param : params) {
                FormParameter formParam = new FormParameter(param);
                map.put(param.getName(), formParam);
            }
        }
        return map;
    }

}
