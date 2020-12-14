package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.service.RunningCase;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class CasesView extends VerticalLayout
        implements ComponentEventListener<ClickEvent<Button>> {

    private final ResourceClient _resClient;
    private final EngineClient _engClient;
    private List<SpecificationData> _specs;
    private List<RunningCase> _cases;

    private Grid<SpecificationData> _specsGrid;
    private Grid<RunningCase> _casesGrid;


    public CasesView(ResourceClient resClient, EngineClient engClient) {
        _resClient = resClient;
        _engClient = engClient;
        _specs = getLoadedSpecifications();
        _cases = getRunningCases();
        add(createSpecsPanel());
        add(createCasesPanel());
        setSizeFull();
    }


    // event occurs when upload dialog closes
    @Override
    public void onComponentEvent(ClickEvent<Button> dialogCloseEvent) {
        _specs = getLoadedSpecifications();     // refresh
        _specsGrid.setItems(_specs);
        _specsGrid.getDataProvider().refreshAll();
    }


    private VerticalLayout createSpecsPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(new H3("Loaded Specifications"));
        panel.add(createSpecsGrid());
        return panel;
    }


    private VerticalLayout createCasesPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.add(new H3("Running Cases"));
        panel.add(createCasesGrid());
        return panel;
    }


    private Grid<SpecificationData> createSpecsGrid() {
        _specsGrid = new Grid<>();
        _specsGrid.setItems(_specs);
        _specsGrid.addColumn(SpecificationData::getSpecURI).setHeader("Name");
        _specsGrid.addColumn(SpecificationData::getSpecVersion).setHeader("Version");
        _specsGrid.addColumn(SpecificationData::getDocumentation).setHeader("Description");
        addSpecGridComponentColumns();
        configureGrid(_specsGrid);
        createSpecsFooter();
        return _specsGrid;
    }


    private Grid<RunningCase> createCasesGrid() {
        _casesGrid = new Grid<>();
        _casesGrid.setItems(_cases);
        _casesGrid.addColumn(RunningCase::getCaseID).setHeader("Case ID");
        _casesGrid.addColumn(RunningCase::getSpecName).setHeader("Specification");
        _casesGrid.addColumn(RunningCase::getSpecVersion).setHeader("Version");
        Grid.Column<RunningCase> cancelColumn = _casesGrid.addComponentColumn(
                item -> createCancelCaseButton(_casesGrid, item));
        GridUtil.configureComponentColumn(cancelColumn);
        configureGrid(_casesGrid);
        return _casesGrid;
    }


    private void addSpecGridComponentColumns() {
        Grid.Column<SpecificationData> launchColumn = _specsGrid.addComponentColumn(
                this::createLaunchSpecButton);
        GridUtil.configureComponentColumn(launchColumn);
        Grid.Column<SpecificationData> removeColumn = _specsGrid.addComponentColumn(
                this::createRemoveSpecButton);
        GridUtil.configureComponentColumn(removeColumn);
    }

    private <T> void configureGrid(Grid<T> grid) {
        grid.setHeightByRows(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        GridUtil.initialSort(grid, 0);
    }


    private void createSpecsFooter() {
        FooterRow footerRow = _specsGrid.appendFooterRow();
        footerRow.getCell(_specsGrid.getColumns().get(0)).setComponent(createAddButton());
    }


    private Button createAddButton() {
        return new Button("Add ...", event -> {
            new UploadDialog(_engClient, _specs, this).open();
        });

    }


    private Button createCancelCaseButton(Grid<RunningCase> grid, RunningCase runningCase) {
        Icon icon = new Icon(VaadinIcon.CLOSE_SMALL);
        icon.setColor("red");
        return new Button(icon, event -> {
            cancelCase(runningCase);
            refreshRunningCases();
        });
    }


    private Button createRemoveSpecButton(SpecificationData spec) {
        Icon icon = new Icon(VaadinIcon.CLOSE_SMALL);
        icon.setColor("red");
        return new Button(icon, event -> {
            if (unloadSpecification(spec.getID())) {
                refreshLoadedSpecifications();
            }
        });
    }


    private Button createLaunchSpecButton(SpecificationData spec) {
        Icon icon = new Icon(VaadinIcon.CHEVRON_CIRCLE_RIGHT);
        icon.setColor("gray");
        return new Button(icon, event -> {
            if (launchCase(spec)) {
                refreshRunningCases();
            }
        });
    }


    private void refreshRunningCases() {
        _cases = getRunningCases();
        _casesGrid.setItems(_cases);
        _casesGrid.getDataProvider().refreshAll();
    }


    private void refreshLoadedSpecifications() {
        _specs = getLoadedSpecifications();
        _specsGrid.setItems(_specs);
        _specsGrid.getDataProvider().refreshAll();
    }


    private boolean launchCase(SpecificationData spec) {
        return true;
    }


    private void cancelCase(RunningCase runningCase) {
        try {
            _engClient.cancelCase(runningCase.getCaseID());
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


    private void showErrorMsg(String msg) {
        new MessageDialog("ERROR: " + StringUtil.unwrap(msg)).open();
    }

}
