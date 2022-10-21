package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;
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
        createContent(p, wir, schema);
    }


    // case start
    public DynForm(EngineClient client, Participant p, List<YParameter> parameters, String schema) {
        super();
        _engClient = client;
        createContent(p, parameters, schema);
    }


    public void addOkListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _okButton.addClickListener(listener);
    }


    public void addSaveListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _saveButton.addClickListener(listener);
    }


    private void createContent(Participant p, WorkItemRecord wir, String schema) {
        DynFormFactory factory = new DynFormFactory(_engClient);
        try {
            DynFormLayout form = factory.makeForm(schema, wir, p);
            setFormHeight(form);
            setWidth(form.getAppropriateWidth());
            setHeader("Edit Work Item " + factory.getFormName(), false);
            addComponent(createScroller(form));
            createButtonsForWorkItem();
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
    }


    private void createContent(Participant p, List<YParameter> parameters,
                                               String schema) {
        DynFormFactory factory = new DynFormFactory(_engClient);
        try {
            DynFormLayout form = factory.makeForm(schema, parameters, p);
            setFormHeight(form);
            setWidth(form.getAppropriateWidth());
            setHeader("Case Start " + factory.getFormName(), false);
            addComponent(createScroller(form));
            createButtonsForCaseStart();
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
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


    private Scroller createScroller(Component form) {
        Scroller scroller = new Scroller(form);
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)");
        return scroller;
    }


    private void setFormHeight(DynFormLayout form) {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details ->
            form.setAppropriateHeight(details.getWindowInnerHeight()));
    }


    private void addButtons(Button... buttons) {
        _okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(buttons);
        getButtonBar().getStyle().set("padding-top", "15px");
    }


    private void pack(VerticalLayout layout) {
        addComponent(layout);
        setWidth(layout.getComponentCount() > 1 ? "700px" : "350px");
        setMaxHeight("75%");
    }

}
