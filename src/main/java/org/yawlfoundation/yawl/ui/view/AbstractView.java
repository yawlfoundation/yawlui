package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.data.provider.SortDirection;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 3/8/2022
 */
abstract class AbstractView extends VerticalLayout {

    protected final ResourceClient _resClient;
    protected final EngineClient _engClient;


    protected AbstractView(ResourceClient resClient, EngineClient engClient) {
        _resClient = resClient;
        _engClient = engClient;
    }

     protected SplitLayout createSplitView(Component top, Component bottom) {
        SplitLayout splitLayout = new SplitLayout(top, bottom);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSizeFull();
        splitLayout.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        return splitLayout;
    }


    protected UnpaddedVerticalLayout createGridPanel(H4 header, Grid<?> grid) {
        UnpaddedVerticalLayout layout = new UnpaddedVerticalLayout("t");
        layout.add(header);
        layout.add(grid);
        return layout;
    }


    protected void addGridFooter(Grid<?> grid, Component... components) {
        FooterRow footerRow = grid.appendFooterRow();
        int lastColIndex = grid.getColumns().size() - 1;
        footerRow.getCell(grid.getColumns().get(lastColIndex)).setComponent(
                new JustifiedButtonLayout(components));
    }


    protected <T> void initialSort(Grid<T> grid, int colIndex) {
         List<GridSortOrder<T>> sortList = new ArrayList<>();
         sortList.add(new GridSortOrder<>(
                         grid.getColumns().get(colIndex), SortDirection.ASCENDING));
         grid.sort(sortList);
     }


     protected <T> void configureActionColumn(Grid.Column<T> column) {
         column.setAutoWidth(true).setFlexGrow(0).setResizable(false)
                 .setSortable(false).setFrozenToEnd(true);
     }


     protected <T> void configureGrid(Grid<T> grid) {
         grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
         grid.getColumns().forEach(col ->
                 col.setResizable(true).setAutoWidth(true).setSortable(true));
         initialSort(grid, 0);
     }


    protected void refreshHeader(H4 header, String text, int count) {
        header.getElement().setText(String.format("%s (%d)", text, count));
    }


    protected void showErrorMsg(String msg) {
        Announcement.error(StringUtil.unwrap(msg));
    }
}
