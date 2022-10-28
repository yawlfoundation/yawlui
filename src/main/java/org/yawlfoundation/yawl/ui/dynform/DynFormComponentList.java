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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a list of components and their combined height
 *
 * Author: Michael Adams
 * Creation Date: 25/02/2008
 */

public class DynFormComponentList extends ArrayList<Component> {

    public DynFormComponentList() {
        super();
    }
    

    public void consolidateChoiceComponents(List<DynFormField> fieldList) {
        Map<String, List<Component>> componentMap = new HashMap<>();
        Map<String, String> paramMap = new HashMap<>();
        for (int i = 0; i < fieldList.size(); i++) {
            DynFormField field = fieldList.get(i);
            if (field.isChoiceField()) {
                List<Component> choices = componentMap.computeIfAbsent(field.getChoiceID(),
                        k -> new ArrayList<>());
                choices.add(this.get(i));

                // all choice components with same choiceID have same param
                paramMap.put(field.getChoiceID(), field.getParam().getValue());
            }
        }

        for (String choiceID : componentMap.keySet()) {
            List<Component> choices = componentMap.get(choiceID);
            int insertionIndex = findInsertionIndex(choices);
            this.removeAll(choices);
            this.add(insertionIndex, new ChoiceComponent(choices, paramMap.get(choiceID)));
        }
    }


    private int findInsertionIndex(List<Component> choices) {
        int index = Integer.MAX_VALUE;
        for (Component choice : choices) {
            index = Math.min(index, this.indexOf(choice));
        }
        return index;
    }
    
}
