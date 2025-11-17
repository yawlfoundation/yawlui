package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.worklet.RaiseExceptionDialog;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.worklet.admin.AdministrationTask;

import java.io.IOException;
import java.util.*;

/**
 * @author Michael Adams
 * @date 3/8/2022
 */
@CssImport(value = "./styles/combo-in-grid.css", themeFor = "vaadin-input-container")
abstract class AbstractGridView<T> extends AbstractView {

    private List<T> _items;
    private Grid<T> _grid;
    private H4 _header;

    private final boolean _showHeader;


    protected AbstractGridView() { this(true); }

    protected AbstractGridView(boolean showHeader) {
        _showHeader = showHeader;
    }


    // part 2 of constructor - to allow subclasses to do any prep first
    protected void build() {
        _items = getItems();
        add(createLayout());
        setSizeFull();
    }

    
    abstract List<T> getItems();

    abstract void addColumns(Grid<T> grid);

    abstract void configureComponentColumns(Grid<T> grid);

    abstract void addItemActions(T item, ActionRibbon ribbon);

    abstract void addFooterActions(ActionRibbon ribbon);

    abstract String getTitle();


    protected Grid<T> getGrid() { return _grid; }


    protected List<T> getLoadedItems() { return _items; }


    protected H4 getHeader() { return _header; }


    protected boolean showHeader() { return _showHeader; }
    

    protected Component createLayout() {
        _header = new H4(String.format("%s (%d)", getTitle(),  _items.size()));
        _grid = createGrid();
        return createGridPanel(_header, _grid);
    }


    protected Grid<T> createGrid() {
        Grid<T> grid = new Grid<>();
        grid.setItems(_items);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        addColumns(grid);
        Grid.Column<T> actionColumn = grid.addComponentColumn(this::createItemActions);
        configureGrid(grid);
        configureActionColumn(actionColumn);
        configureComponentColumns(grid);
        addGridFooter(grid, createFooterActions());
        return grid;
    }


    protected ActionRibbon createItemActions(T item) {
        ActionRibbon ribbon = new ActionRibbon();
        addItemActions(item, ribbon);
        return ribbon;
    }


    protected ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        addFooterActions(ribbon);
        return ribbon;
    }


    protected void timedRefresh(UI ui, long msecs) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (ui != null) {
                    ui.access(() -> refresh());
                }
            }
        };
        new Timer().schedule(task, msecs);
    }


    protected void refresh() {
        _items = getItems();
        _grid.setItems(_items);
        _grid.getDataProvider().refreshAll();
        _grid.recalculateColumnWidths();
        if (showHeader()) {
            refreshHeader(_header, getTitle(), _items.size());
        }
    }


    protected ActionIcon createAddAction(
                ComponentEventListener<ClickEvent<Icon>> listener) {
        return new ActionIcon(VaadinIcon.PLUS, null, "Add", listener);
    }


    protected ActionIcon createEditAction(
                ComponentEventListener<ClickEvent<Icon>> listener) {
        return new ActionIcon(VaadinIcon.PENCIL, null, "Edit", listener);
    }


    protected ActionIcon createMultiDeleteAction(
                ComponentEventListener<ClickEvent<Icon>> listener) {
        return createDeleteAction("Remove Selected", listener);
    }


    protected ActionIcon createDeleteAction(
                ComponentEventListener<ClickEvent<Icon>> listener) {
        return createDeleteAction("Remove", listener);
    }


    protected ActionIcon createDeleteAction(String tooltip,
            ComponentEventListener<ClickEvent<Icon>> listener) {
        return new ActionIcon(VaadinIcon.CLOSE_SMALL, ActionIcon.RED,
                tooltip, listener);
    }


    protected ActionIcon createRefreshAction() {
        return new ActionIcon(VaadinIcon.REFRESH, null, "Refresh",
                event -> refresh());
    }


    protected UnpaddedVerticalLayout createGridPanel(H4 header, Grid<?> grid) {
        UnpaddedVerticalLayout layout = new UnpaddedVerticalLayout("t");
        if (showHeader()) {
            layout.add(header);
        }
        layout.add(grid);
        layout.setSizeFull();
        return layout;
    }


    protected void addGridFooter(Grid<?> grid, Component... components) {
        FooterRow footerRow = grid.appendFooterRow();
        int lastColIndex = grid.getColumns().size() - 1;
        footerRow.getCell(grid.getColumns().get(lastColIndex)).setComponent(
                new JustifiedButtonLayout(components));
    }


    protected void initialSort(Grid<T> grid, int colIndex) {
        List<GridSortOrder<T>> sortList = new ArrayList<>();
        sortList.add(new GridSortOrder<>(
                grid.getColumns().get(colIndex), SortDirection.ASCENDING));
        grid.sort(sortList);
    }


    protected void configureActionColumn(Grid.Column<T> column) {
        column.setAutoWidth(true).setFlexGrow(0).setResizable(false)
                .setSortable(false).setFrozenToEnd(true);
    }


    protected void configureGrid(Grid<T> grid) {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.getColumns().forEach(col ->
                col.setResizable(true).setAutoWidth(true).setSortable(true));
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        initialSort(grid, 0);
    }

    
    protected ComboBox<Participant> buildParticipantCombo(List<Participant> pList) {
        sortParticipantList(pList);
        ComboBox<Participant> comboBox = new ComboBox<>();
        comboBox.setItems(pList);
        comboBox.setItemLabelGenerator(Participant::getFullName);
        comboBox.setPlaceholder(pList.size() + " participants");
        comboBox.addValueChangeListener(e -> {
            comboBox.setValue(null);
            comboBox.setPlaceholder(pList.size() + " participants");
        });

        comboBox.getElement().getStyle().set("padding", "0");
        comboBox.getElement().setAttribute("theme", "transparent");
        return comboBox;
    }


    protected void sortParticipantList(List<Participant> pList) {
        pList.sort(Comparator.comparing(Participant::getLastName)
                .thenComparing(Participant::getFirstName));
    }


    protected void addWorkletAdministrationTask(AdministrationTask task) {
        try {
            getWorkletClient().addWorkletAdministrationTask(task);
        }
        catch (IOException e) {
            announceError("Failed to raise Exception: " + e.getMessage());
        }
    }


    protected void raiseExternalException(String id) {
        raiseExternalException("Case", id, null);
    }


    protected void raiseExternalException(WorkItemRecord wir) {
        raiseExternalException("Item", wir.getID(), wir.getCaseID());
    }


    // for Case=level, id == caseID, id2 == null
    // for item-level, id == itemID, id2 == caseID
    private void raiseExternalException(String level, String id, String id2) {
        String title = String.format("Raise External Exception for %s: %s", level, id);
        List<String> triggers = getExternalTriggers(level, id);
        RaiseExceptionDialog dialog = new RaiseExceptionDialog(title, triggers);
        dialog.getOKButton().addClickListener(e -> {
            if (dialog.isNewException()) {
                if (dialog.validate()) {
                    String newTitle = dialog.getHeading();
                    String scenario = dialog.getScenario();
                    String process = dialog.getProcess();
                    String caseID = level.equals("Case") ? id : id2;
                    String itemID = level.equals("Case") ? null : id;
                    addNewExceptionTask(level, caseID, itemID, newTitle, scenario, process);
                }
            }
            else {
                String selected = dialog.getSelection();
                if (!selected.equals("None")) {
                    raiseException(level, id, selected);
                }
            }
            dialog.close();
            refresh();
            Announcement.success("Exception raised");
        });
        dialog.open();
    }


    private void raiseException(String level, String id, String trigger) {
        try {
            if (level.equals("Case")) {
                getWorkletClient().raiseCaseExternalException(id, trigger);
            }
            else {
               getWorkletClient().raiseItemExternalException(id, trigger);
            }
        }
        catch (IOException e) {
            announceError("Failed to raise Exception: " + e.getMessage());
        }
    }


    private void addNewExceptionTask(String level, String caseID, String itemID,
                                     String title, String scenario, String process) {
        AdministrationTask task = level.equals("Case") ?
                new AdministrationTask(caseID, title, scenario, process,
                        AdministrationTask.TASKTYPE_CASE_EXTERNAL_EXCEPTION) :
                new AdministrationTask(caseID, itemID, title, scenario, process,
                        AdministrationTask.TASKTYPE_ITEM_EXTERNAL_EXCEPTION);

        addWorkletAdministrationTask(task);
    }


    private List<String> getExternalTriggers(String level, String id) {
        try {
            return level.equals("Case") ?
                    getWorkletClient().getExternalTriggersForCase(id) :
                    getWorkletClient().getExternalTriggersForItem(id);
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }


    protected String getHeadersAsCSV() {
        StringBuilder builder = new StringBuilder();
        for (Grid.Column<T> column : _grid.getColumns()) {
            if (column.getHeaderComponent() == null) continue;
            String headerText = column.getHeaderComponent().getElement().toString();
            if (builder.length() > 0) builder.append(',');
            builder.append(StringUtil.unwrap(headerText));
        }
        builder.append('\n');
        return builder.toString();
    }

}
