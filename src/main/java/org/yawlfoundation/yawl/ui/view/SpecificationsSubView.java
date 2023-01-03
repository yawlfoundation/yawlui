package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.DelayedStartDialog;
import org.yawlfoundation.yawl.ui.dialog.SpecInfoDialog;
import org.yawlfoundation.yawl.ui.dialog.upload.UploadSpecificationDialog;
import org.yawlfoundation.yawl.ui.dynform.DynForm;
import org.yawlfoundation.yawl.ui.listener.DelayedCaseLaunchedListener;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class SpecificationsSubView extends AbstractGridView<SpecificationData> {

    private final List<DelayedCaseLaunchedListener> _listeners = new ArrayList<>();
    private final AbstractView _parent;

    public SpecificationsSubView(AbstractView parent) {
        super();
        _parent = parent;
        build();
    }


    @Override
    List<SpecificationData> getItems() {
        try {
             return getResourceClient().getLoadedSpecificationData();
         }
         catch (IOException ioe) {
             announceError(ioe.getMessage());
             return Collections.emptyList();
         }
    }


    @Override
    void addColumns(Grid<SpecificationData> grid) {
        grid.addColumn(SpecificationData::getSpecURI).setHeader(
                UiUtil.bold("Name"));
        grid.addColumn(SpecificationData::getSpecVersion).setHeader(
                UiUtil.bold("Version"));
        grid.addColumn(SpecificationData::getDocumentation).setHeader(
                UiUtil.bold("Description"));
    }


    @Override
    void configureComponentColumns(Grid<SpecificationData> grid) { }    // no cols to configure


    @Override
    void addItemActions(SpecificationData item, ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start", event ->
            launchCase(item));
        ribbon.add(VaadinIcon.CLOCK, ActionIcon.GREEN, "Start later", event -> {
            delayedStart(item);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.INFO, "Info", event -> {
            showInfo(item);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.DOWNLOAD_ALT, "Download log", event -> {
            downloadLog(item);
            ribbon.reset();
         });
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload", event -> {
            if (unloadSpecification(item)) {
                refresh();
            }
        });
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            new UploadSpecificationDialog(getLoadedItems(), e -> refresh()).open();
            ribbon.reset();
        }));

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload Selected",
                event -> {
                    getGrid().getSelectedItems().forEach(this::unloadSpecification);
                    refresh();
                });

        ribbon.add(createRefreshAction());
    }


    @Override
    String getTitle() {
        return "Specifications";
    }


    public void addDelayedCaseLaunchListener(DelayedCaseLaunchedListener listener) {
        _listeners.add(listener);
    }


    private boolean unloadSpecification(SpecificationData spec) {
        boolean success = unloadSpecification(spec.getID());
        if (success) {
             Announcement.success("Specification " +
                     spec.getID().toString() + " unloaded");
        }
        return success;
    }


    private void launchCase(SpecificationData spec) {
        try {
            if (isLatestLoadedVersion(spec)) {
                List<YParameter> inputParams = spec.getInputParams();
                if (! (inputParams.isEmpty() || spec.hasExternalCaseDataGateway())) {
                    launchCase(spec, inputParams, -1);
                }
                else {
                    launchCase(spec.getID(), null);
                }
            }
        }
        catch (ResourceGatewayException | IOException e) {
            announceError(e.getMessage());
        }
    }


    private void launchCase(SpecificationData spec, List<YParameter> inputParams,
                            long delay)
            throws ResourceGatewayException, IOException {
        String schema = getResourceClient().getCaseParamsDataSchema(spec.getID());
        DynForm dynForm = new DynForm(inputParams, schema);
        dynForm.addOkListener(e -> {
            if (dynForm.validate()) {
                String caseData = dynForm.generateOutputData();
                if (delay > 0) {
                    launchCase(spec.getID(), caseData, delay);
                }
                else {
                    launchCase(spec.getID(), caseData);
                }
                dynForm.close();
                refresh();
            }
        });
        dynForm.open();
    }


    private void launchCase(YSpecificationID specID, String caseData) {
        try {
            String caseID = getEngineClient().launchCase(specID, caseData);
            Announcement.success("Case " + caseID + " launched");
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
        }
    }


    private void launchCase(YSpecificationID specID, String caseData, long delay) {
        try {
            getEngineClient().launchCase(specID, caseData, delay);
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
        }
    }


    private void delayedStart(SpecificationData spec) {
        if (isLatestLoadedVersion(spec)) {
            DelayedStartDialog dialog = new DelayedStartDialog();
            dialog.addOkClickListener(e -> {
                long delay = dialog.getDelay();
                dialog.close();
                if (delay > -1) {
                    List<YParameter> inputParams = spec.getInputParams();
                    try {
                        if (! (inputParams.isEmpty() || spec.hasExternalCaseDataGateway())) {
                            launchCase(spec, inputParams, delay);
                        }
                        else {
                            launchCase(spec.getID(), null, delay);
                        }
                        announceDelayedCaseLaunch(delay);
                    }
                    catch (ResourceGatewayException | IOException ioe) {
                        announceError(ioe.getMessage());
                    }
                }
            });
            dialog.open();
        }
    }

    
    private boolean isLatestLoadedVersion(SpecificationData spec) {
        YSpecificationID specID = spec.getID();
        for (SpecificationData item : getLoadedItems()) {
            if (specID.isPreviousVersionOf(item.getID())) {
                Announcement.error("Unable to start case. Only the latest version of a " +
                                "specification may be launched. The latest loaded version " +
                                "of this specification is '" + item.getSpecVersion() +
                                "', the selected version is '" + spec.getSpecVersion() +
                                "'. Please select the latest version.");
                return false;
            }
        }
        return true;
    }


    private boolean unloadSpecification(YSpecificationID specID) {
        try {
            return getEngineClient().unloadSpecification(specID);
        }
        catch (IOException ioe) {
            Announcement.warn(ioe.getMessage());
            return false;
        }
    }


    private void showInfo(SpecificationData specData) {
        new SpecInfoDialog(specData).open();
    }


    private void downloadLog(SpecificationData specData) {
        try {
            String log = getResourceClient().getMergedXESLog(
                    specData.getID(), true);
            if (log.isEmpty()) {
                Announcement.highlight("No cases for selected specification");
                return;
            }

            String fileName = String.format("%s_v%s.xes", specData.getSpecURI(),
                                        specData.getSpecVersion());

            // file download doesn't work from this (inner) view, so we need to call it
            // from the parent (outer) view
            _parent.downloadFile(fileName, log);

            Announcement.success("XES log downloaded");
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
        }
    }


    private void announceDelayedCaseLaunch(long delay) {
        _listeners.forEach(l -> l.delayedCaseLaunched(delay + 1000));
    }

}
