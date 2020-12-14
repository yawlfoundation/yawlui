package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 2/12/20
 */
public class GridUtil {

    public static <T> void initialSort(Grid<T> grid, int colIndex) {
        List<GridSortOrder<T>> sortList = new ArrayList<>();
        sortList.add(new GridSortOrder<>(
                        grid.getColumns().get(colIndex), SortDirection.ASCENDING));
        grid.sort(sortList);
    }


    public static <T> void configureComponentColumn(Grid.Column<T> column) {
        column.setWidth("60px").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
    }

}
