package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;
import org.yawlfoundation.yawl.ui.layout.VerticalScrollLayout;
import org.yawlfoundation.yawl.ui.service.EngineClient;

import java.util.List;

/**
 * @author Michael Adams
 * @date 6/10/2022
 */
public class DynForm extends AbstractDialog {

    private Button _okButton;  // 'Complete' or 'Start'
    private Button _saveButton;
    private final Button _cancelButton = new Button("Cancel", event -> close());

    private final EngineClient _engClient;


    // work item edit
    public DynForm(EngineClient client, Participant p, WorkItemRecord wir, String schema) {
        super();
        _engClient = client;
        VerticalScrollLayout layout = createContent(p, wir, schema);
        addComponent(layout);
        createButtonsForWorkItem();
        setWidth(layout.getContent().getComponentCount() > 1 ? "700px" : "350px");
        setMaxHeight("75%");
    }


    // case start
    public DynForm(EngineClient client, Participant p, List<YParameter> parameters, String schema) {
        super("Case Parameters");
        _engClient = client;
        addComponent(createContent(p, parameters, schema));
        createButtonsForCaseStart();
        setWidth("700px");
    }


    public void addOkListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _okButton.addClickListener(listener);
    }


    public void addSaveListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _saveButton.addClickListener(listener);
    }


    private VerticalScrollLayout createContent(Participant p, WorkItemRecord wir,
                                               String schema) {
        VerticalScrollLayout layout = createContentLayout();
        DynFormFactory factory = new DynFormFactory(_engClient);
        try {
            layout.add(factory.makeForm(schema, wir, p));
            setHeader("Edit Work Item " + factory.getFormName());
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
        return layout;
    }


    private VerticalScrollLayout createContent(Participant p, List<YParameter> parameters,
                                               String schema) {
        VerticalScrollLayout layout = createContentLayout();
        DynFormFactory factory = new DynFormFactory(_engClient);
        try {
            layout.add(factory.makeForm(schema, parameters, p));
            setHeader("Case Start " + factory.getFormName());
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
        return layout;
    }


    private VerticalScrollLayout createContentLayout() {
        VerticalScrollLayout layout = new VerticalScrollLayout();
        layout.setPadding(false);
        layout.setMargin(false);
        return layout;
    }

    private void createButtonsForWorkItem() {
        _saveButton = new Button("Save");
        _okButton = new Button("Complete");
        addButtons(_cancelButton, _saveButton, _okButton);
    }


    private void createButtonsForCaseStart() {
        _okButton = new Button("Start");
        addButtons(_cancelButton, _okButton);
    }


    private void addButtons(Button... buttons) {
        _okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(buttons);
        getButtonBar().getStyle().set("padding-top", "15px");
    }

}
