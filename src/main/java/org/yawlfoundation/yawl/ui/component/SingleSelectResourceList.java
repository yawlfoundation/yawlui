package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;

import java.util.List;

/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class SingleSelectResourceList extends ListBox<AbstractResourceAttribute> {

    public SingleSelectResourceList(List<AbstractResourceAttribute> items) {
        setItems(items);
        setWidth("210px");
        setItemLabelGenerator(AbstractResourceAttribute::getName);
        getElement().getStyle().set("overflow-y", "scroll");
        getElement().getStyle().set("flex-grow", "1");
        getElement().getStyle().set("border", "1px solid lightgray");
    }

}
