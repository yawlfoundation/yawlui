package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.worklet.RejectWorkletDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.*;
import org.yawlfoundation.yawl.ui.util.InstalledServices;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.worklet.admin.AdministrationTask;
import org.yawlfoundation.yawl.worklet.selection.WorkletRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class CasesSubView extends AbstractGridView<RunningCase> {

    public CasesSubView() {
        super();

        // update grid when a new case is launched
        getEngineClient().addEventListener(e -> {
            if (e.getAction() == ClientEvent.Action.LaunchCase) {
                refresh();
            }
        });

        build();
    }


    @Override
    List<RunningCase> getItems() {
        try {
            return getEngineClient().getRunningCases();
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
            return Collections.emptyList();
        }
    }


    @Override
    void addColumns(Grid<RunningCase> grid) {
        grid.addColumn(RunningCase::getCaseID).setHeader(UiUtil.bold("Case ID"));
        grid.addColumn(RunningCase::getSpecName).setHeader(UiUtil.bold("Specification"));
        grid.addColumn(RunningCase::getSpecVersion).setHeader(UiUtil.bold("Version"));
    }


    @Override
    void configureComponentColumns(Grid<RunningCase> grid) { }            // no cols to configure


    @Override
    void addItemActions(RunningCase item, ActionRibbon ribbon) {
        boolean hasWorklets = new InstalledServices().hasWorkletService();
        if (hasWorklets) {
            ActionIcon exceptionAction = ribbon.add(VaadinIcon.EXCLAMATION_CIRCLE_O,
                    ActionIcon.RED, "Worklet Actions", null);
            ContextMenu menu = new ContextMenu(exceptionAction);
            menu.setOpenOnClick(true);
            menu.addItem("Raise Exception", event -> {
                raiseExternalException(item.getCaseID());
                ribbon.reset();
            });
            menu.addItem("Reject Worklet", event -> rejectWorklet(item.getCaseID()));
        }

        ActionIcon cancelIcon = ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED,
                "Cancel", event -> {
            cancelCase(item);
            refresh();
        });
        if (! hasWorklets) {
            cancelIcon.insertBlank();      // add space to left
        }
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Cancel Selected",
                 event -> {
                     getGrid().getSelectedItems().forEach(this::cancelCase);
                     refresh();
                 });

        ribbon.add(createRefreshAction());
    }

    
    @Override
    String getTitle() {
        return "Cases";
    }


    private void cancelCase(RunningCase runningCase) {
        try {
            getEngineClient().cancelCase(runningCase.getCaseID());
            Announcement.success("Case " + runningCase.getCaseID() + " cancelled");
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
        }
    }


    private boolean isRunningWorklet(String caseID) {
        try {
            List<WorkletRunner> runners = getWorkletClient().getRunningWorklets();
            for (WorkletRunner runner : runners) {
                if (runner.getCaseID().equals(caseID)) {
                    return true;
                }
            }
        }
        catch (IOException ex) {
            //
        }
        return false;
    }


    private void rejectWorklet(String caseID) {
        if (! isRunningWorklet(caseID)) {
            Announcement.warn("Case %s is not a running worklet", caseID);
            return;
        }

        RejectWorkletDialog dialog = new RejectWorkletDialog(caseID);
        dialog.getOKButton().addClickListener(e -> {
            if (dialog.validate()) {
                String heading = dialog.getHeading();
                String scenario = dialog.getScenario();
                String process = dialog.getProcess();
                AdministrationTask task = new AdministrationTask(caseID, heading,
                        scenario, process, AdministrationTask.TASKTYPE_REJECTED_SELECTION);
                if (rejectWorklet(task)) {
                    dialog.close();
                    refresh();
                }
            }
        });
        dialog.open();
    }


    private boolean rejectWorklet(AdministrationTask task) {
        try {
            getWorkletClient().addWorkletAdministrationTask(task);
            Announcement.success("Worklet rejected");
            return true;
        }
        catch (IOException ex) {
            Announcement.error("Failed to reject worklet: " + ex);
            return false;
        }
    }

}
