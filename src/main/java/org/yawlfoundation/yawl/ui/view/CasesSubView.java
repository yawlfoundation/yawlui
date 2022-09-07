package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ClientEvent;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.service.RunningCase;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class CasesSubView extends AbstractGridView<RunningCase> {

    public CasesSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);

        // update grid when a new case is launched
        engClient.addEventListener(e -> {
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
        ActionIcon icon = ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED,
                "Cancel", event -> {
            cancelCase(item);
            refresh();
        });
        icon.insertBlank();
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

}
