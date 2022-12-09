package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.List;

/**
 * @author Michael Adams
 * @date 19/9/2022
 */
public abstract class AbstractTabbedView extends AbstractView {

    protected final VerticalLayout content = new VerticalLayout();
    protected final Participant _participant;

    public AbstractTabbedView(Participant participant) {
        super();
        _participant = participant;
        add(createLayout());
        setSizeFull();
    }


    abstract List<Tab> getTabs();

    abstract void setContent(Tab tab);

    @Override
    Component createLayout() {
        Tabs tabs = new Tabs();   
        for (Tab tab : getTabs()) {
           enlargeLabel(tab);
           tabs.add(tab);
        }
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            setContent(event.getSelectedTab());
        });

        content.setSpacing(false);
        content.setSizeFull();
        setContent(tabs.getSelectedTab());
        VerticalLayout layout = new VerticalLayout(tabs, content);
        layout.setSizeFull();
        return layout;
    }


    protected VerticalLayout getContent() { return content; }

    protected Participant getParticipant() { return _participant; }


    private void enlargeLabel(Tab tab) {
        tab.getStyle().set("font-size", "var(--lumo-font-size-l)");
    }

}
