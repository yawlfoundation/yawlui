package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.resourcing.calendar.CalendarEntry;
import org.yawlfoundation.yawl.resourcing.calendar.ResourceCalendar;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResource;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanResource;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.component.Prompt;
import org.yawlfoundation.yawl.ui.dialog.CalendarDialog;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 14/11/2022
 */
public class CalendarView extends AbstractGridView<CalendarEntry> {

    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.systemDefault().getRules()
            .getOffset(LocalDateTime.now());

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_AND_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm (yyyy-MM-dd)");

    private final ComboBox<Resource> _selectResourceCombo = new ComboBox<>();
    private final ComboBox<String> _filterCombo = new ComboBox<>();

    private final List<Participant> _participants;
    private final List<NonHumanResource> _assets;
    private final Participant _user;

    private ActionIcon _addIcon;
    private LocalDate _viewDate;
    private String _resourceID;
    private ResourceCalendar.ResourceGroup _resourceGroup;

    public CalendarView(ResourceClient resClient, Participant currentUser) {
        super(resClient, null, false);
        _user = currentUser;
        _participants = getParticipants();
        _assets = getAssets();
        _viewDate = LocalDate.now(ZoneId.systemDefault());
        build();
    }


    @Override
    protected UnpaddedVerticalLayout createGridPanel(H4 header, Grid<?> grid) {
        UnpaddedVerticalLayout layout = new UnpaddedVerticalLayout("t");
        layout.add(new H4(getTitle()));
        layout.add(createFilterBar());
        layout.add(grid);
        layout.setSizeFull();
        return layout;
    }


    @Override
    List<CalendarEntry> getItems() {
        return getEntries();
    }


    @Override
    void addColumns(Grid<CalendarEntry> grid) {
        grid.addColumn(this::renderStartTime).setHeader(UiUtil.bold("Start Time"));
        grid.addColumn(this::renderEndTime).setHeader(UiUtil.bold("End Time"));
        grid.addColumn(this::renderResource).setHeader(UiUtil.bold("Resource"));
        grid.addColumn(CalendarEntry::getStatus).setHeader(UiUtil.bold("Status"));
        grid.addColumn(CalendarEntry::getWorkload).setHeader(UiUtil.bold("Workload"));
        grid.addColumn(CalendarEntry::getComment).setHeader(UiUtil.bold("Comment"));
    }


    @Override
    void configureComponentColumns(Grid<CalendarEntry> grid) { }


    @Override
    void addItemActions(CalendarEntry item, ActionRibbon ribbon) {
        ActionIcon editAction = ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
            CalendarDialog dialog = new CalendarDialog(getSelectedResourceLabel(), item);
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    CalendarEntry entry = dialog.getEntry();
                    entry.setAgent(getAgent());
                    updateEntry(entry);
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });

        editAction.insertBlank();

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
            removeEntry(item);
            ribbon.reset();
            refresh();
        });
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        _addIcon = createAddAction(event -> {
            CalendarDialog dialog = new CalendarDialog(getSelectedResourceLabel());
            dialog.initPickers(_viewDate.atTime(9, 0, 0));
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    CalendarEntry entry = dialog.getEntry();
                    entry.setResourceID(getSelectedResourceID());
                    entry.setAgent(getAgent());
                    addEntry(entry, dialog.getRepeat(), dialog.getRepeatUntil());
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });
        _addIcon.setEnabled(false);

        ribbon.add(_addIcon);

        ribbon.add(createMultiDeleteAction(
                event -> {
                    removeEntries(getGrid().getSelectedItems());
                    ribbon.reset();
                    refresh();
                }));

        ribbon.add(createRefreshAction());
    }


    @Override
    String getTitle() {
        return "Calendar";
    }


    private HorizontalLayout createFilterBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(createDateChooser());
        layout.add(createFilterLayout());
        return layout;
    }

    private HorizontalLayout createDateChooser() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(false);

        DatePicker picker = new DatePicker(LocalDate.now());
        picker.addValueChangeListener(e -> {
            _viewDate = e.getValue();
            refresh();
        });

        ActionIcon decDateAction = new ActionIcon(VaadinIcon.ANGLE_LEFT, null,
                "Previous day", e -> {
            LocalDate current = picker.getValue();
            picker.setValue(current.minusDays(1));
        });
        configActionIcon(decDateAction);

        ActionIcon incDateAction = new ActionIcon(VaadinIcon.ANGLE_RIGHT, null,
                "Next day", e -> {
            LocalDate current = picker.getValue();
            picker.setValue(current.plusDays(1));
        });
        configActionIcon(incDateAction);

        layout.add(decDateAction, picker, incDateAction);
        return layout;
    }


    private void configActionIcon(ActionIcon icon) {
        icon.setSize("24px");
        icon.getStyle().set("margin-top", "7px");
    }


    private HorizontalLayout createFilterLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(new Prompt("Filter:"));
        layout.add(createFilterCombo());
        layout.add(createFilterSelectionCombo());
        return layout;
    }

    private ComboBox<String> createFilterCombo() {
        List<String> items = List.of("None", "All Resources", "All Participants",
                "All Assets", "Select Participant", "Select Asset");
        _filterCombo.setItems(items);
        _filterCombo.setValue("None");
        _filterCombo.addValueChangeListener(e -> {
            _addIcon.setEnabled(! e.getValue().equals("None"));
             switch (e.getValue()) {
                 case "None" :
                     setResourceGroup(null);
                     break;
                 case "All Resources" :
                     setResourceGroup(ResourceCalendar.ResourceGroup.AllResources);
                     break;
                 case "All Participants" :
                     setResourceGroup(ResourceCalendar.ResourceGroup.HumanResources);
                     break;
                 case "All Assets" :
                     setResourceGroup(ResourceCalendar.ResourceGroup.NonHumanResources);
                     break;
                 case "Select Participant" :
                     primeSelectResourceCombo(_participants);
                     break;
                 case "Select Asset" :
                     primeSelectResourceCombo(_assets);
                     break;
             }
        });
        return _filterCombo;
    }


    private HorizontalLayout createFilterSelectionCombo() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(false);
        layout.setPadding(false);
        layout.add(new Prompt("Resource: "));
        layout.add(_selectResourceCombo);

        _selectResourceCombo.setEnabled(false);
        _selectResourceCombo.addValueChangeListener(e -> setResourceID(e.getValue().id));

        return layout;
    }
    

    private void primeSelectResourceCombo(List<? extends AbstractResource> resources) {
        List<Resource> items = new ArrayList<>();
        resources.forEach(resource -> items.add(new Resource(resource)));
        _selectResourceCombo.setItems(items);
        _selectResourceCombo.setItemLabelGenerator(item -> item.label);
        if (! items.isEmpty()) {
            _selectResourceCombo.setValue(items.get(0));
        }
        _selectResourceCombo.setEnabled(true);
    }


    private String getSelectedResourceLabel() {
        String filterValue = _filterCombo.getValue();
        if (filterValue.startsWith("All")) {
            return filterValue;
        }
        return _selectResourceCombo.getValue().label;
    }


    private String getSelectedResourceID() {
        switch (_filterCombo.getValue()) {
            case "All Resources": return "ALL_RESOURCES";
            case "All Participants": return "ALL_HUMAN_RESOURCES";
            case "Al Assets": return "ALL_NONHUMAN_RESOURCES";
            default: return _selectResourceCombo.getValue().id;
        }
    }


    private String getAgent() {
        return _user != null ? _user.getUserID() : "admin";
    }


    private void setResourceID(String id) {
        _resourceGroup = null;
        _resourceID = id;
        refresh();
    }


    private void setResourceGroup(ResourceCalendar.ResourceGroup group) {
        _resourceID = null;
        _resourceGroup = group;
        _selectResourceCombo.setEnabled(false);
        refresh();
    }


    private String renderStartTime(CalendarEntry entry) {
        return longToDateTimeString(entry.getStartTime());
    }


    private String renderEndTime(CalendarEntry entry) {
        return longToDateTimeString(entry.getEndTime());
    }


    private String longToDateTimeString(long msecs) {
        LocalDateTime dateTime = milliToDateTime(msecs);
        LocalDate datePart = dateTime.toLocalDate();
        return _viewDate.isEqual(datePart) ?
                TIME_FORMATTER.format(dateTime) :             // same date, so time only
                TIME_AND_DATE_FORMATTER.format(dateTime);     // time [and date]
    }


    private LocalDateTime milliToDateTime(long msecs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(msecs), ZoneId.systemDefault());
    }

    private long dateTimeToMilli(LocalDateTime dateTime) {
        return dateTime.toEpochSecond(ZONE_OFFSET) * 1000;
    }

    private String renderResource(CalendarEntry entry) {
        String id = entry.getResourceID();
        if (id.startsWith("ALL_")) {
            switch (id) {
                case "ALL_RESOURCES" : return "All Resources";
                case "ALL_HUMAN_RESOURCES" : return "All Participants";
                case "ALL_NONHUMAN_RESOURCES" : return "All Assets";
            }
        }
        for (Participant p : _participants) {
            if (p.getID().equals(id)) {
                return p.getFullName();
            }
        }
        for (NonHumanResource asset : _assets) {
            if (asset.getID().equals(id)) {
                return asset.getName();
            }
        }
        return "Unknown";
    }


    private void addEntry(CalendarEntry entry, String repeat, LocalDate until) {
        if (repeat.equals("None")) {
            addEntry(entry);
        }
        else {
           LocalDate startDate = milliToDateTime(entry.getStartTime()).toLocalDate();
            Period period;
            switch (repeat) {
                case "Weekly" : period = Period.ofWeeks(1); break;
                case "Monthly" : period = Period.ofMonths(1); break;
                default : period = Period.ofDays(1); break;
            }
            addEntries(entry, startDate, until, period);
        }
    }


    private void addEntries(CalendarEntry entry, LocalDate startDate, LocalDate until,
                            Period period) {
        startDate.datesUntil(until, period).forEach(date -> {
            addEntry(entry);
            LocalDateTime startDateTime = milliToDateTime(entry.getStartTime());
            LocalDateTime endDateTime = milliToDateTime(entry.getEndTime());
            entry.setStartTime(dateTimeToMilli(startDateTime.plus(period)));
            entry.setEndTime(dateTimeToMilli(endDateTime.plus(period)));
        });
    }


    private List<CalendarEntry> getEntries() {
        long from = dateTimeToMilli(_viewDate.atStartOfDay());
        long to = dateTimeToMilli(_viewDate.atTime(23, 59, 59));
        try {
            if (_resourceGroup != null) {
                return getResourceClient().getCalendarEntries(_resourceGroup, from, to);
            }
            else {
                return getResourceClient().getCalendarEntries(_resourceID, from, to);
            }
        }
        catch (IOException e) {
            Announcement.error("Failed to retrieve calendar entries: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    private boolean addEntry(CalendarEntry entry) {
        try {
            getResourceClient().addCalendarEntry(entry);
            String date = milliToDateTime(entry.getStartTime()).toLocalDate().toString();
            Announcement.success("Calendar entry for start date %s added", date);
            return true;
        }
        catch (IOException e) {
            Announcement.error("Failed to add calendar entry: " + e.getMessage());
        }
        return false;
    }


    private boolean updateEntry(CalendarEntry entry) {
        try {
            if (getResourceClient().updateCalendarEntry(entry)) {
                String date = milliToDateTime(entry.getStartTime()).toLocalDate().toString();
                Announcement.success("Calendar entry for start date %s added", date);
                return true;
            }
            return false;
        }
        catch (IOException e) {
            Announcement.error("Failed to update calendar entry: " + e.getMessage());
        }
        return false;
    }


    private boolean removeEntry(CalendarEntry entry) {
        try {
            if (getResourceClient().deleteCalendarEntry(entry.getEntryID())) {
                String date = milliToDateTime(entry.getStartTime()).toLocalDate().toString();
                Announcement.success("Calendar entry for start date %s removed", date);
                return true;
            }
            return false;
        }
        catch (IOException e) {
            Announcement.error("Failed to delete calendar entry: " + e.getMessage());
        }
        return false;
    }


    private boolean removeEntries(Set<CalendarEntry> items) {
        boolean success = true;
        for (CalendarEntry item : items) {
            success = removeEntry(item) & success;
        }
        return success;
    }


    private List<Participant> getParticipants() {
        try {
            return getResourceClient().getParticipants();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to retrieve participant list: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    private List<NonHumanResource> getAssets() {
        try {
            return getResourceClient().getNonHumanResources();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error("Failed to retrieve asset list: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    static class Resource {
        String id;
        String label;

        Resource(AbstractResource r) {
            id = r.getID();
            label = r.getName();
        }
    }

}
