package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.LogStatistics;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 24/10/2025
 */
public class LogStatisticsSubView extends AbstractGridView<Pair<String, String>> {

    private final List<Pair<String, String>> _items;
    private final LogStatistics _logStatistics;
    private final YSpecificationID _specID;
    private LogView _parent;


    public LogStatisticsSubView(LogView parent, YSpecificationID specID) {
        _parent = parent;
        _specID = specID;
        _logStatistics = getStatistics(specID);
        _items = createItems(_logStatistics);
        build();
    }


    @Override
    List<Pair<String, String>> getItems() {
        return _items;
    }

    @Override
    void addColumns(Grid<Pair<String, String>> grid) {
        grid.addColumn(Pair::getLeft).setHeader(UiUtil.bold("Key"));
        grid.addColumn(Pair::getRight).setHeader(UiUtil.bold("Value"));
    }

    @Override
    void configureComponentColumns(Grid<Pair<String, String>> grid) {

    }

    @Override
    void addItemActions(Pair<String, String> item, ActionRibbon ribbon) {

    }

    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.FILE_TABLE, "Download as CSV",
               event -> {
                   String fileName = String.format("%s_%s_statistics.csv",
                           _specID.getUri(), _specID.getVersionAsString());
                   _parent.downloadFile(fileName, getAsCSV());
               });
    }

    @Override
    String getTitle() {
        return "Summary Statistics for Specification: " + _specID.toString();
    }


    private LogStatistics getStatistics(YSpecificationID specID) {
        try {
            return getLogClient().getStatistics(specID, -1, -1);
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return new LogStatistics();
        }
    }


    private List<Pair<String, String>> createItems(LogStatistics logStatistics) {
        List<Pair<String, String>> items = new ArrayList<>();
        items.add(new ImmutablePair<>("A. Specification", logStatistics.getSpecID()));
        items.add(new ImmutablePair<>("B. Started", String.valueOf(logStatistics.getStarted())));
        items.add(new ImmutablePair<>("C. Completed", String.valueOf(logStatistics.getCompleted())));
        items.add(new ImmutablePair<>("D. Cancelled", String.valueOf(logStatistics.getCancelled())));
        items.add(new ImmutablePair<>("E. Min time to completion", logStatistics.getMinCompletion()));
        items.add(new ImmutablePair<>("F. Max time to completion", logStatistics.getMaxCompletion()));
        items.add(new ImmutablePair<>("G. Mean time to completion", logStatistics.getAvgCompletion()));
        items.add(new ImmutablePair<>("H. Min time to cancellation", logStatistics.getMinCancelled()));
        items.add(new ImmutablePair<>("I. Max time to cancellation", logStatistics.getMaxCancelled()));
        items.add(new ImmutablePair<>("J. Mean time to cancellation", logStatistics.getAvgCancelled()));
        return items;
    }


    protected String getAsCSV() {
        StringBuilder builder = new StringBuilder();
        builder.append("Key,Value\n");
        for (Pair<String,String> pair : getItems()) {
            builder.append(pair.getLeft()).append(',')
                    .append(pair.getRight()).append('\n');
        }
        return builder.toString();
    }


}
