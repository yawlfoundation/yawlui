package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.resourcing.datastore.eventlog.ResourceEvent;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 28/10/2025
 */
public class LogResourceEventView extends AbstractGridView<ResourceEvent> {

    private final List<ResourceEvent> _eventList;
    private final LogView _parent;
    private final Participant _participant;
    private final boolean _showResourceIdColumn;
    private final String _title;

    private String _exportFileName;


    public LogResourceEventView(LogView parent, Participant p,
                                List<ResourceEvent> events,
                                boolean showParticipantColumn,
                                String title) {
        super();
        _parent = parent;
        _participant = p;
        _eventList = new ArrayList<>(events);
        _showResourceIdColumn = showParticipantColumn;
        _title = title;
        build();
    }


    protected String getAsCSV() {
        StringBuilder builder = new StringBuilder();
        builder.append(getHeadersAsCSV());
        for (ResourceEvent row : getItems()) {
            builder.append(toCSV(row));
        }
        return builder.toString();
    }


    @Override
    List<ResourceEvent> getItems() {
        return _eventList;
    }


    @Override
    void addColumns(Grid<ResourceEvent> grid) {
        grid.addColumn(ResourceEvent::get_caseID).setHeader(UiUtil.bold("Case ID"));
        grid.addColumn(ResourceEvent::getTimeStampString).setHeader(UiUtil.bold("Timestamp"));
        grid.addColumn(ResourceEvent::get_taskID).setHeader(UiUtil.bold("Task ID"));
        grid.addColumn(ResourceEvent::get_itemID).setHeader(UiUtil.bold("Item ID"));
        grid.addColumn(ResourceEvent::get_event).setHeader(UiUtil.bold("Event"));
        if (_showResourceIdColumn) {
            grid.addColumn(ResourceEvent::get_resourceID).setHeader(UiUtil.bold("Participant"));
        }
    }

    @Override
    void configureComponentColumns(Grid<ResourceEvent> grid) {
        
    }

    @Override
    void addItemActions(ResourceEvent item, ActionRibbon ribbon) {

    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        if (_participant != null) {
            ribbon.add(VaadinIcon.FILE_TABLE, "Download as CSV",
                    event -> {
                        if (_exportFileName == null) {
                            _exportFileName = "Event History for " + _participant.getFullName();
                        }
                        _parent.downloadFile(_exportFileName + ".csv", getAsCSV());
                    });
        }
    }


    @Override
    String getTitle() {
        String name = _participant != null ? _participant.getFullName() : "";
        return _title + name;
    }


    public void setExportFileName(String fileName) {
        _exportFileName = fileName;
    }


    // ordering must match column order
    public String toCSV(ResourceEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(appendCheck(event.get_caseID())).append(',');
        builder.append(appendCheck(event.getTimeStampString())).append(',');
        builder.append(appendCheck(event.get_taskID())).append(',');
        builder.append(appendCheck(event.get_itemID())).append(',');
        builder.append(appendCheck(event.get_event()));
        if (_showResourceIdColumn) {
            builder.append(',').append(appendCheck(event.get_resourceID()));
        }
        builder.append('\n');
        return builder.toString();
    }


    private String appendCheck(String item) {
        return item != null ? item :  "";
    }

}
