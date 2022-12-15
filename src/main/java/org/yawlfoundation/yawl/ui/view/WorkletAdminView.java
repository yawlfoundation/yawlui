package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.worklet.admin.AdministrationTask;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 25/11/2022
 */
public class WorkletAdminView extends AbstractGridView<AdministrationTask> {

    private final HorizontalLayout _content = new HorizontalLayout();
    private final VerticalLayout _details;

    private final H5 detailsHeader = new H5();
    private final TextField titleField = new TextField("Title");
    private final TextArea scenarioArea = new TextArea("Description");
    private final TextArea processArea = new TextArea("Suggested Process");
    private final Button closeBtn = new Button("Close");
    private final ActionIcon refreshAction = createRefreshAction();
    private ActionIcon viewAction;
    private ActionIcon completeAction;
    private ActionIcon multiCompleteAction;


    public WorkletAdminView() {
        super();
        build();
        _details = createDetailsLayout();
    }

    
    @Override
    protected HorizontalLayout createLayout() {
        Component gridPanel = super.createLayout();
        _content.add(gridPanel);
        _content.setFlexGrow(1, gridPanel);
        _content.setSizeFull();
        return _content;
    }


    @Override
    List<AdministrationTask> getItems() {
        return getTasks();
    }

    @Override
    void addColumns(Grid<AdministrationTask> grid) {
        grid.addColumn(AdministrationTask::getCaseID).setHeader(UiUtil.bold("Case ID"));
        grid.addColumn(AdministrationTask::getItemID).setHeader(UiUtil.bold("Item ID"));
        grid.addColumn(this::renderTaskType).setHeader(UiUtil.bold("Type"));
        grid.addColumn(AdministrationTask::getTitle).setHeader(UiUtil.bold("Title"));

    }

    @Override
    void configureComponentColumns(Grid<AdministrationTask> grid) { }

    @Override
    void addItemActions(AdministrationTask item, ActionRibbon ribbon) {
        viewAction = new ActionIcon(VaadinIcon.GLASSES, null, "View Details",
                event ->  viewDetails(item));
        completeAction = new ActionIcon(VaadinIcon.CHECK, ActionIcon.GREEN, "Complete",
                event -> {
                    completeTask(item);
                    refresh();
                });
        ribbon.add(viewAction, completeAction);
    }

    @Override
    void addFooterActions(ActionRibbon ribbon) {
        multiCompleteAction = new ActionIcon(VaadinIcon.CHECK, ActionIcon.GREEN,
                "Complete", event -> {
            getGrid().getSelectedItems().forEach(this::completeTask);
            refresh();
        });
        ribbon.add(multiCompleteAction, refreshAction);
    }


    @Override
    String getTitle() {
        return "Worklet Administration Tasks";
    }


    private VerticalLayout createDetailsLayout() {
        titleField.setReadOnly(true);
        scenarioArea.setReadOnly(true);
        processArea.setReadOnly(true);
        titleField.setWidthFull();
        scenarioArea.setWidthFull();
        processArea.setWidthFull();
        closeBtn.getStyle().set("margin-left", "auto");
        closeBtn.addClickListener(e -> {
            _content.remove(_details);
            enableGridActions(true);
        });
        VerticalLayout layout = new VerticalLayout(detailsHeader, titleField,
                scenarioArea, processArea, closeBtn);
        layout.setWidthFull();
        layout.setHeightFull();
        return layout;
    }

    private void viewDetails(AdministrationTask task) {
        String itemID = task.getItemID();
        String header = "Task Details for " + (itemID != null ? "Item " + itemID :
                "Case " + task.getCaseID());
        detailsHeader.setText(header);
        titleField.setValue(task.getTitle());
        scenarioArea.setValue(task.getScenario());
        processArea.setValue(task.getProcess());
        _content.add(_details);
        enableGridActions(false);
    }


    private void enableGridActions(boolean enable) {
        viewAction.setEnabled(enable);
        completeAction.setEnabled(enable);
        multiCompleteAction.setEnabled(enable);
        refreshAction.setEnabled(enable);
    }

    
    private List<AdministrationTask> getTasks() {
        try {
            return getWorkletClient().getWorkletAdministrationTasks();
        }
        catch (IOException e) {
            Announcement.warn("Failed to retrieve tasks from service: " + e);
        }
        return Collections.emptyList();
    }


    private void completeTask(AdministrationTask task) {
        try {
            getWorkletClient().removeWorkletAdministrationTask(task.getID());
            Announcement.success("Task completed");
        }
        catch (IOException e) {
            Announcement.error("Failed to complete task: " + e);
        }
    }


    private String renderTaskType(AdministrationTask task) {
        switch (task.getTaskType()) {
            case AdministrationTask.TASKTYPE_REJECTED_SELECTION: return "Worklet Rejection";
            case AdministrationTask.TASKTYPE_CASE_EXTERNAL_EXCEPTION: return "Case Exception";
            case AdministrationTask.TASKTYPE_ITEM_EXTERNAL_EXCEPTION: return "Item Exception";
            default: return "Undefined";
        }
    }
}
