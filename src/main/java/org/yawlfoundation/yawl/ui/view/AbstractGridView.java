package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
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
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Michael Adams
 * @date 3/8/2022
 */
@CssImport(value = "./styles/combo-in-grid.css", themeFor = "vaadin-input-container")
abstract class AbstractGridView<T> extends AbstractView {

    private List<T> _items;
    private Grid<T> _grid;
    private H4 _header;


    protected AbstractGridView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
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


    protected void refresh() {
        _items = getItems();
        _grid.setItems(_items);
        _grid.getDataProvider().refreshAll();
        _grid.recalculateColumnWidths();
        refreshHeader(_header, getTitle(), _items.size());
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
        layout.add(header);
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
}
