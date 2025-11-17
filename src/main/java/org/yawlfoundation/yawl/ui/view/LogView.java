package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.resourcing.datastore.eventlog.ResourceEvent;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.SingleSelectSpecificationIdList;
import org.yawlfoundation.yawl.ui.dialog.SelectParticipantDialog;
import org.yawlfoundation.yawl.ui.dialog.SingleValueDialog;
import org.yawlfoundation.yawl.ui.dialog.SpecificationIdDialog;
import org.yawlfoundation.yawl.ui.util.ParticipantFieldTransposer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public class LogView extends AbstractView {

    private final LogInputSubView _inputView;
    private final VerticalLayout _emptyLayout = new VerticalLayout();
    private Component _currentSecondaryView = _emptyLayout;
    private SplitLayout _splitView;
    private List<YSpecificationID> _fullIdList;

    private LogXESView _xesView;
    private LogStatisticsSubView _statsView;
    private LogSpecificationIdSubView _specIDView;
    private LogResourceEventView _resourceView;


    public LogView() {
        super();
        _inputView = new LogInputSubView(this);
        add(createLayout());
        setSizeFull();
    }


    protected void generateOutputView(LogInputSubView inputView) {
        LogViewType selectedType = inputView.getSelectedLog();
        switch (selectedType) {
            case ParticipantHistory: generateParticipantHistoryView(); return;
            case CaseEvents: generateCaseEventsView(); return;
            case CasesInvolvingParticipant: generateCasesWithParticipantView(); return;
//            case TaskStatistics: generateTaskStatistics(); return;
        }
        
        final SingleSelectSpecificationIdList.Versions versions;
        switch (selectedType) {
            case SpecificationStatistics:
            case Specification: versions = SingleSelectSpecificationIdList.Versions.Single; break;
            case SpecificationAllVersions: versions = SingleSelectSpecificationIdList.Versions.All; break;
            default: versions = null;
        }
        if (selectedType == LogViewType.SpecificationIds) {
            _specIDView = new LogSpecificationIdSubView(this);
            changeOutputView(_specIDView.createLayout());
        }
        else {
            SpecificationIdDialog dialog = new SpecificationIdDialog(
                    getSpecificationIds(versions), versions,
                    ! (selectedType == LogViewType.SpecificationStatistics));

            dialog.addOkClickListener(e -> {
                YSpecificationID selected = dialog.getSelected();

                // if statistics
                if (selectedType == LogViewType.SpecificationStatistics) {
                    _statsView = new LogStatisticsSubView(this, selected);
                    changeOutputView(_statsView.createLayout());
                }
                else {
                    _xesView = new LogXESView(this,
                            getSelectedSpecIds(selected, versions),
                            versions, dialog.getSelectedResourceNameFormat());
                    changeOutputView(_xesView.createLayout());
                }
                dialog.close();
            });
            dialog.open();
        }
    }


    @Override
    Component createLayout() {
        _splitView = createSplitView(_inputView.createLayout(), _emptyLayout);
        return _splitView;
    }

// todo : deal with underscores in task names and new view needed for output
//    private void generateTaskStatistics() {
//        final SingleSelectSpecificationIdList.Versions versions =
//                SingleSelectSpecificationIdList.Versions.Single;
//        SpecificationIdDialog dialog = new SpecificationIdDialog(
//                getSpecificationIds(versions), versions, false);
//        dialog.addOkClickListener(e -> {
//            YSpecificationID selected = dialog.getSelected();
//            String taskName = "Give_advice";
//            try {
//                 List<ResourceEvent> events = getResourceClient()
//                         .getTaskStatistics(selected, taskName);
//                System.out.println(events);
//                 String title = "All Case Events for Participant: ";
//                 _resourceView = new LogResourceEventView(this, selected, events,
//                         false, title);
//                 changeOutputView(_resourceView.createLayout());
//             }
//             catch (IOException ex) {
//                 Announcement.error(ex.getMessage());
//             }
//             dialog.close();
//         });
//         dialog.open();
//
//    }


    private void generateParticipantHistoryView() {
        try {
            SelectParticipantDialog dialog = new SelectParticipantDialog(
                    getResourceClient().getParticipants());
            dialog.addOkClickListener(e -> {
                Participant selected = dialog.getSelected();
                try {
                    List<ResourceEvent> events = getResourceClient()
                            .getParticipantHistory(selected.getID());
                    String title = "All Case Events for Participant: ";
                    _resourceView = new LogResourceEventView(this, selected, events,
                            false, title);
                    changeOutputView(_resourceView.createLayout());
                }
                catch (IOException ex) {
                    Announcement.error(ex.getMessage());
                }
                dialog.close();
            });
            dialog.open();
        }
        catch (Exception e) {
            Announcement.error(e.getMessage());
        }
    }


    private void generateCaseEventsView() {
        try {
            SingleValueDialog dialog = new SingleValueDialog("Enter Case Id", "");
            dialog.getOKButton().addClickListener(e -> {
                String caseID = dialog.getValue();
                try {
                    List<ResourceEvent> events = getResourceClient().getCaseEvents(caseID);
                    String title = "All Events for Case: " + caseID;
                    _resourceView = new LogResourceEventView(this, null, events,
                            false, title);
                    _resourceView.setExportFileName("Events for Case " + caseID);
                    changeOutputView(_resourceView.createLayout());
                }
                catch (IOException ex) {
                    Announcement.error(ex.getMessage());
                }
                dialog.close();
            });
            dialog.open();
        }
        catch (Exception e) {
            Announcement.error(e.getMessage());
        }
    }


    private void generateCasesWithParticipantView() {
        try {
            SelectParticipantDialog dialog = new SelectParticipantDialog(
                    getResourceClient().getParticipants(), true);
            dialog.addOkClickListener(e -> {
                Participant selected = dialog.getSelected();
                try {
                    List<ResourceEvent> events = getResourceClient()
                            .getCaseHistoryInvolvingParticipant(selected.getID());
                    transposeResourceIDs(events, dialog.getSelectedResourceNameFormat());
                    String title = "All Case Events for cases involving Participant: ";
                    _resourceView = new LogResourceEventView(this, selected, events,
                            true, title);
                    changeOutputView(_resourceView.createLayout());
                }
                catch (IOException ex) {
                    Announcement.error(ex.getMessage());
                }
                dialog.close();
            });
            dialog.open();
        }
        catch (Exception e) {
            Announcement.error(e.getMessage());
        }
    }

    private void changeOutputView(Component newView) {
        _splitView.remove(_currentSecondaryView);
        _currentSecondaryView = newView;
        _splitView.addToSecondary(newView);
    }

    
    private List<YSpecificationID> getSpecificationIds(SingleSelectSpecificationIdList.Versions versions) {
        try {
            _fullIdList = getLogClient().getAllSpecifications();
            if (versions == SingleSelectSpecificationIdList.Versions.Single) {
                return _fullIdList;
            }

            List<YSpecificationID> uniqueList = new ArrayList<>();
            List<String> uniqueUriList =  new ArrayList<>();
            for (YSpecificationID id : _fullIdList) {
                String uri = id.getUri();
                if (!uniqueUriList.contains(id.getUri())) {
                    uniqueUriList.add(uri);
                    uniqueList.add(id);
                }
            }
            return uniqueList;
        }
        catch (IOException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }


    private List<YSpecificationID> getSelectedSpecIds(YSpecificationID selected,
                                                      SingleSelectSpecificationIdList.Versions version) {
        List<YSpecificationID> selectedList = new ArrayList<>();
        if (version == SingleSelectSpecificationIdList.Versions.Single) {
            selectedList.add(selected);
        }
        else {
            for (YSpecificationID id : _fullIdList) {
                if (id.getUri().equals(selected.getUri())) {
                    selectedList.add(id);
                }
            }
        }
        return selectedList;
    }


    private void transposeResourceIDs(List<ResourceEvent> events,
                                      ParticipantFieldTransposer.Field selectedFormat) {
        if (selectedFormat == ParticipantFieldTransposer.Field.Key) {
            return;                   // no change
        }
        try {
            ParticipantFieldTransposer fieldTransposer = new ParticipantFieldTransposer(
                    getResourceClient().getParticipants());
            fieldTransposer.setFields(ParticipantFieldTransposer.Field.Key, selectedFormat);
            for (ResourceEvent event : events) {
                event.set_resourceID(fieldTransposer.transpose(event.get_resourceID()));
            }
        }
        catch (Exception e) {
            Announcement.warn("Unable to transpose resource ids: " +e.getMessage());
        }
    }
    
}
