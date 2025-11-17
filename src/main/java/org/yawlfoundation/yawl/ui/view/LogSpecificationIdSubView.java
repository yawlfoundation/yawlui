package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 24/10/2025
 */
public class LogSpecificationIdSubView extends AbstractGridView<YSpecificationID> {

    private final LogView _parent;
    private final List<YSpecificationID> _items;

    public LogSpecificationIdSubView(LogView parent) {
        _parent = parent;
        _items = getItems();
        build();
    }

    @Override
    List<YSpecificationID> getItems() {
        try {
             return getLogClient().getAllSpecifications();
         }
         catch (IOException ioe) {
             announceError(ioe.getMessage());
             return Collections.emptyList();
         }
    }

    @Override
    void addColumns(Grid<YSpecificationID> grid) {
        grid.addColumn(YSpecificationID::getUri).setHeader(UiUtil.bold("Name (uri)"));
        grid.addColumn(YSpecificationID::getVersionAsString).setHeader(UiUtil.bold("Version"));
        grid.addColumn(YSpecificationID::getIdentifier).setHeader(UiUtil.bold("Key"));
    }

    @Override
    void configureComponentColumns(Grid<YSpecificationID> grid) {

    }

    @Override
    void addItemActions(YSpecificationID item, ActionRibbon ribbon) {

    }

    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.FILE_TABLE, "Download as CSV",
              event -> {
                  String fileName = "specification_identifiers.csv";
                  _parent.downloadFile(fileName, getAsCSV());
              });
    }

    @Override
    String getTitle() {
        return "All Logged Specification Identifiers";
    }


    protected String getAsCSV() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name (uri),Version,Key\n");
        for (YSpecificationID specID : _items) {
            builder.append(specID.getUri()).append(',')
                    .append(specID.getVersionAsString()).append(',')
                    .append(specID.getIdentifier()).append('\n');
        }
        return builder.toString();
    }

}
