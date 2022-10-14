package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;

/**
 * @author Michael Adams
 * @date 13/10/2022
 */
public class DynFormLayout extends FormLayout {

    @Override
    public void add(Component... components) {
        super.add(components);
        for (Component c : components) {
            if ((c instanceof SubPanel)) {
                setColspan(c, 2);
            }
            else {
                setColspan(c, 1);
            }
        }
    }
}
