package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.listbox.ListBox;
import org.yawlfoundation.yawl.engine.YSpecificationID;

import java.util.List;

/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class SingleSelectSpecificationIdList extends ListBox<YSpecificationID> {

    public enum Versions { Single, All }

    public SingleSelectSpecificationIdList(List<YSpecificationID> items, Versions versions) {
        setItems(items);
        setWidth("460px");
        getElement().getStyle().set("overflow-y", "scroll");
        getElement().getStyle().set("flex-grow", "1");
        getElement().getStyle().set("border", "1px solid lightgray");
        switch (versions) {
            case Single : setItemLabelGenerator(YSpecificationID::toString); break;
            case All    : setItemLabelGenerator(YSpecificationID::getUri); break;
        }
    }

}
