package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.DelayedStartDialog;
import org.yawlfoundation.yawl.ui.dialog.SpecInfoDialog;
import org.yawlfoundation.yawl.ui.dialog.UploadDialog;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.service.RunningCase;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class CasesView extends AbstractView {

    private List<SpecificationData> _specs;
    private List<RunningCase> _cases;

    private H4 _specHeader;
    private H4 _casesHeader;
    private Grid<SpecificationData> _specsGrid;
    private Grid<RunningCase> _casesGrid;


    public CasesView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
        _specs = getLoadedSpecifications();
        _cases = getRunningCases();
        add(createSplitView(createSpecsPanel(), createCasesPanel()));
        setSizeFull();
    }


    private UnpaddedVerticalLayout createSpecsPanel() {
        _specHeader = new H4("Specifications (" + _specs.size() + ")");
        return createGridPanel(_specHeader, createSpecsGrid());
    }


    private UnpaddedVerticalLayout createCasesPanel() {
        _casesHeader = new H4("Cases (" + _cases.size() + ")");
        return createGridPanel(_casesHeader, createCasesGrid());
    }


    private Grid<SpecificationData> createSpecsGrid() {
        _specsGrid = new Grid<>();
        _specsGrid.setItems(_specs);
        _specsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        _specsGrid.addColumn(SpecificationData::getSpecURI).setHeader(
                UiUtil.bold("Name"));
        _specsGrid.addColumn(SpecificationData::getSpecVersion).setHeader(
                UiUtil.bold("Version"));
        _specsGrid.addColumn(SpecificationData::getDocumentation).setHeader(
                UiUtil.bold("Description"));
        Grid.Column<SpecificationData> actionColumn = _specsGrid.addComponentColumn(
                this::createSpecActions);
        configureGrid(_specsGrid);
        configureActionColumn(actionColumn);
        addGridFooter(_specsGrid, createSpecFooterActions());
        return _specsGrid;
    }


    private Grid<RunningCase> createCasesGrid() {
        _casesGrid = new Grid<>();
        _casesGrid.setItems(_cases);
        _casesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        _casesGrid.addColumn(RunningCase::getCaseID).setHeader(
                UiUtil.bold("Case ID"));
        _casesGrid.addColumn(RunningCase::getSpecName).setHeader(
                UiUtil.bold("Specification"));
        _casesGrid.addColumn(RunningCase::getSpecVersion).setHeader(
                UiUtil.bold("Version"));
        Grid.Column<RunningCase> actionColumn = _casesGrid.addComponentColumn(
                this::createCancelCaseAction);
        configureGrid(_casesGrid);
        configureActionColumn(actionColumn);
        addGridFooter(_casesGrid, createCaseFooterActions());
        return _casesGrid;
    }
    

    private ActionRibbon createSpecFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.PLUS, "Add", event -> {
            new UploadDialog(_engClient, _specs,
                    e -> refreshSpecifications()).open();
            ribbon.reset();
        });

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload Selected",
                event -> {
                    _specsGrid.getSelectedItems().forEach(this::unloadSpecification);
                    refreshSpecifications();
                });

        ribbon.add(VaadinIcon.REFRESH, "Refresh", event ->
                refreshSpecifications());

        return ribbon;
    }


    private ActionRibbon createCaseFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Cancel Selected",
                event -> {
                    _casesGrid.getSelectedItems().forEach(this::cancelCase);
                    refreshCases();
                });

        ribbon.add(VaadinIcon.REFRESH, "Refresh", event -> refreshCases());

        return ribbon;
    }


    private ActionRibbon createCancelCaseAction(RunningCase runningCase) {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Cancel", event -> {
            cancelCase(runningCase);
            refreshCases();
        });
        return ribbon;
    }


    private ActionRibbon createSpecActions(SpecificationData spec) {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start", event -> {
            if (launchCase(spec)) {
                refreshCases();
             }
        });
        ribbon.add(VaadinIcon.CLOCK, ActionIcon.GREEN, "Start later", event -> {
            delayedStart(spec);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.INFO, "Info", event -> {
            showInfo(spec);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.DOWNLOAD_ALT, "Download log", event -> {
            downloadLog(spec);
            ribbon.reset();
         });
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload", event -> {
            if (unloadSpecification(spec)) {
                refreshSpecifications();
            }
        });
        return ribbon;
    }


    private boolean unloadSpecification(SpecificationData spec) {
        boolean success = unloadSpecification(spec.getID());
        if (success) {
             Announcement.success("Specification " +
                     spec.getID().toString() + " unloaded");
        }
        return success;
    }
    

    private void refreshCases() {
        _cases = getRunningCases();
        _casesGrid.setItems(_cases);
        _casesGrid.getDataProvider().refreshAll();
        _casesGrid.recalculateColumnWidths();
        refreshHeader(_casesHeader, "Cases", _cases.size());
    }


    private void refreshSpecifications() {
        _specs = getLoadedSpecifications();
        _specsGrid.setItems(_specs);
        _specsGrid.getDataProvider().refreshAll();
        _specsGrid.recalculateColumnWidths();
        refreshHeader(_specHeader, "Specifications", _specs.size());
    }


    private boolean launchCase(SpecificationData spec) {
        try {
            String caseID = _engClient.launchCase(spec.getID(), null);
            Announcement.success("Case " + caseID + " cancelled");
            return true;
        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
            return false;
        }
    }


    private void cancelCase(RunningCase runningCase) {
        try {
            _engClient.cancelCase(runningCase.getCaseID());
            Announcement.success("Case " + runningCase.getCaseID() + " cancelled");
        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
        }
    }


    private boolean unloadSpecification(YSpecificationID specID) {
        try {
            return _engClient.unloadSpecification(specID);
        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
            return false;
        }
    }


    private List<SpecificationData> getLoadedSpecifications() {
        try {
            return _resClient.getLoadedSpecificationData();
        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
            return Collections.emptyList();
        }
    }


    private List<RunningCase> getRunningCases() {
        try {
            return _engClient.getRunningCases();
        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
            return Collections.emptyList();
        }
    }


    private void showInfo(SpecificationData specData) {
        new SpecInfoDialog(specData).open();
    }


    private void downloadLog(SpecificationData specData) {
        try {
            String log = _resClient.getMergedXESLog(specData.getID(), true);
            if (log.isEmpty()) {
                Announcement.highlight("No cases for selected specification");
                return;
            }

            String fileName = String.format("%s_v%s.xes", specData.getSpecURI(),
                                        specData.getSpecVersion());
            InputStreamFactory isFactory = () -> new ByteArrayInputStream(
                    log.getBytes(StandardCharsets.UTF_8));
            StreamResource resource = new StreamResource(fileName, isFactory);
            resource.setContentType("text/xml");
            resource.setCacheTime(0);
            resource.setHeader("Content-Disposition",
                    "attachment;filename=\"" + fileName + "\"");

            Anchor downloadAnchor = new Anchor(resource, "");
            Element element = downloadAnchor.getElement();
            element.setAttribute("download", true);
            element.getStyle().set("display", "none");
            add(downloadAnchor);

            // simulate a click & remove anchor after file downloaded
            element.executeJs("return new Promise(resolve =>{this.click(); " +
                    "setTimeout(() => resolve(true), 150)})", element)
                    .then(jsonValue -> {
                        remove(downloadAnchor);
                        Announcement.success("XES log downloaded");
                    });

        }
        catch (IOException ioe) {
            showErrorMsg(ioe.getMessage());
        }
    }


    private void delayedStart(SpecificationData spec) {
        DelayedStartDialog dialog = new DelayedStartDialog();
        dialog.addOkClickListener(e -> {
            long delay = dialog.getDelay();
            if (delay > -1) {
                try {
                    _engClient.launchCase(spec.getID(), null, delay);
                }
                catch (IOException ioe) {
                    showErrorMsg(ioe.getMessage());
                }
                dialog.close();
            }
        });
        dialog.open();
    }

}
