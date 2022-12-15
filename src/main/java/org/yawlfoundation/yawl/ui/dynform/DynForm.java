package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.Scroller;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;
import org.yawlfoundation.yawl.ui.service.Clients;
import org.yawlfoundation.yawl.ui.util.ApplicationProperties;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.List;

/**
 * @author Michael Adams
 * @date 6/10/2022
 */
public class DynForm extends AbstractDialog {

    private Button _okButton;  // 'Complete' or 'Start'
    private Button _saveButton;
    private final Button _cancelButton = new Button("Cancel", event -> cancelform());

    private final DynFormFactory _factory = new DynFormFactory();


    // work item edit
    public DynForm(Participant p, WorkItemRecord wir, String schema) {
        super();
        createContent(p, wir, schema);
    }


    // case start
    public DynForm(List<YParameter> parameters, String schema) {
        super();
        createContent(parameters, schema);
    }


    public void addOkListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _okButton.addClickListener(listener);
    }


    public void addSaveListener(ComponentEventListener<ClickEvent<Button>> listener) {
        _saveButton.addClickListener(listener);
    }

    public boolean validate() {
        return _factory.validate();
    }


    public String generateOutputData() {
        return _factory.getDataList();
    }


    private void createContent(Participant p, WorkItemRecord wir, String schema) {
        try {
            DynFormLayout form = _factory.createForm(schema, wir, p);
            configureForm(form, "Edit Work Item");
            createButtonsForWorkItem();
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
    }


    private void createContent(List<YParameter> parameters, String schema) {
        try {
            DynFormLayout form = _factory.createForm(schema, parameters, null);
            configureForm(form, "Case Start");
            createButtonsForCaseStart();
        }
        catch (DynFormException dfe) {
            Announcement.warn("Failed to create form: " + dfe.getMessage());
        }
    }


    private void configureForm(DynFormLayout form, String header) {
        setFormHeight(form);
        setWidth(form.getAppropriateWidth());
        setHeader(header);
        addFormName();
        addComponent(createScroller(form));
        applyUserDefinedStyles(form);
        UiUtil.setFocus(form);
    }


    private void addFormName() {
        H5 name = new H5(_factory.getFormName());
        name.getStyle().set("margin-bottom", "20px");
        addComponent(name);
    }


    private void createButtonsForWorkItem() {
        _saveButton = new Button("Save");
        _okButton = new Button("Complete");
        addButtons(_cancelButton, _saveButton, _okButton);
        assignShortcutKey();
    }


    private void createButtonsForCaseStart() {
        _okButton = new Button("Start");
        addButtons(_cancelButton, _okButton);
        assignShortcutKey();
    }


    private void assignShortcutKey() {
        DynFormEnterKeyAction action = ApplicationProperties.getDynFormEnterKeyAction();
        if (action == DynFormEnterKeyAction.SAVE && _saveButton != null) {
            _saveButton.addClickShortcut(Key.ENTER);
        }
        else if (! (action == DynFormEnterKeyAction.NONE)) {
            _okButton.addClickShortcut(Key.ENTER);
        }
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


    private void applyUserDefinedStyles(DynFormLayout form) {
        String imageURI = _factory.getPageBackgroundURL();
        if (imageURI != null) {
            form.getStyle().set("background-image", "url(\"" + imageURI + "\")");
        }
        else {
            String bgColour = _factory.getPageBackgroundColour();
            if (bgColour != null) {
                form.getStyle().set("background-color", bgColour);
            }
        }
    }


    private void cancelform() {
        removeDocsOnCancel();
        close();
    }


    private void removeDocsOnCancel() {
        List<Long> docIDs = _factory.getDocComponentIDs();
        if (! docIDs.isEmpty()) {
            try {
                for (Long docID : docIDs) {

                    // an id of -1 means no doc was uploaded by the doc component
                    if (docID > -1) Clients.getDocStoreClient().removeStoredDocument(docID);
                }
            }
            catch (IOException ioe) {
                // nothing more can be done
            }
        }
    }
    
}
