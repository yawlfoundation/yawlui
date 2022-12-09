package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import org.yawlfoundation.yawl.resourcing.calendar.CalendarEntry;
import org.yawlfoundation.yawl.ui.view.CalendarView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @author Michael Adams
 * @date 15/11/2022
 */
public class CalendarDialog extends AbstractDialog {

    private final DateTimePicker _startPicker = new DateTimePicker("Start");
    private final DateTimePicker _endPicker = new DateTimePicker("End");
    private final DatePicker _untilPicker = new DatePicker("Until");
    private final ComboBox<String> _repeatCombo = new ComboBox<>("Repeat");
    private final IntegerField _workloadField = new IntegerField("Workload");
    private final TextArea _commentField = new TextArea("Comment");
    private final Button _okButton = new Button("OK");

    private CalendarEntry _entry;

    private enum Mode { Add, Edit}

    // adding
    public CalendarDialog(String resourceLabel) {
        super("Add Calendar Entry for: " + resourceLabel);
        init(Mode.Add);
    }

    //editing
    public CalendarDialog(String resourceLabel, CalendarEntry entry) {
        super("Edit Calendar Entry for: " + resourceLabel);
        _entry = entry;
        init(Mode.Edit);
        populateForm(entry);
    }


    public void initPickers(LocalDateTime dateTime) {
        LocalDateTime start = dateTime.isAfter(LocalDateTime.now()) ?
                dateTime : LocalDateTime.now();

        // round start time up to next hour
        start = start.plusMinutes(59).truncatedTo(ChronoUnit.HOURS);

        _startPicker.setValue(start);
        _endPicker.setValue(start.plusHours(1));
    }


    public Button getOkButton() { return _okButton; }


    public boolean validate() {
        return true;
    }


    // rest of entry details get added in the calling view
    public CalendarEntry getEntry() {
        if (_entry == null) {
            _entry = new CalendarEntry();
        }
        _entry.setStartTime(_startPicker.getValue().toEpochSecond(CalendarView.ZONE_OFFSET) * 1000);
        _entry.setEndTime(_endPicker.getValue().toEpochSecond(CalendarView.ZONE_OFFSET) * 1000);
        _entry.setWorkload(_workloadField.getValue());
        _entry.setComment(_commentField.getValue());
        return _entry;
    }


    public String getRepeat() { return _repeatCombo.getValue(); }

    public LocalDate getRepeatUntil() { return _untilPicker.getValue(); }


    private void init(Mode mode) {
        configFields();
        addComponent(createForm(mode));
        createButtons(_okButton);
        setWidth("500px");
    }


    private FormLayout createForm(Mode mode) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.add(_startPicker, 2);
        form.add(_endPicker, 2);
        if (mode == Mode.Add) {
            form.add(_repeatCombo, 1);
            form.add(_untilPicker, 1);
        }
        form.add(_workloadField, 1);
        form.add(_commentField, 2);

        _startPicker.focus();
        return form;
    }


    private void configFields() {
        _startPicker.setValue(LocalDateTime.now());
        _startPicker.addValueChangeListener(e -> validateStartTime());
        _endPicker.setValue(LocalDateTime.now().plusHours(1));
        _endPicker.addValueChangeListener(e -> validateEndTime());
        _untilPicker.setEnabled(false);
        _workloadField.setValue(100);
        configRepeatCombo();
        configWorkloadField();
    }


    private void configRepeatCombo() {
        List<String> items = List.of("None", "Daily", "Weekly", "Monthly");
        _repeatCombo.setItems(items);
        _repeatCombo.setValue("None");
        _repeatCombo.addValueChangeListener(e -> {
            _untilPicker.setEnabled(! e.getValue().equals("None"));
            switch (e.getValue()) {
                case "Daily" : _untilPicker.setValue(_startPicker.getValue()
                        .plusDays(1).toLocalDate()); break;
                case "Weekly" : _untilPicker.setValue(_startPicker.getValue()
                        .plusWeeks(1).toLocalDate()); break;
                case "Monthly" : _untilPicker.setValue(_startPicker.getValue()
                        .plusMonths(1).toLocalDate()); break;
            }
        });
    }


    private void configWorkloadField() {
        _workloadField.setMin(1);
        _workloadField.setMax(100);
        _workloadField.setHasControls(true);

        Div suffix = new Div();
        suffix.setText("%");
        _workloadField.setSuffixComponent(suffix);
    }


    private void populateForm(CalendarEntry entry) {
        _startPicker.setValue(longToDateTime(entry.getStartTime()));
        _endPicker.setValue(longToDateTime(entry.getEndTime()));
        _workloadField.setValue(entry.getWorkload());
        if (entry.getComment() != null) {
            _commentField.setValue(entry.getComment());
        }
    }


    private void validateStartTime() {
        LocalDateTime start = _startPicker.getValue();
        LocalDateTime end = _endPicker.getValue();
        _endPicker.setInvalid(false);
        _startPicker.setInvalid(false);
        if (end.isBefore(start)) {
            _startPicker.setErrorMessage("Start must be before end");
            _startPicker.setInvalid(true);
        }
        _okButton.setEnabled(! (_startPicker.isInvalid() || _endPicker.isInvalid()));
    }


    private void validateEndTime() {
        LocalDateTime start = _startPicker.getValue();
        LocalDateTime end = _endPicker.getValue();
        _startPicker.setInvalid(false);
        _endPicker.setInvalid(false);
        if (end.isBefore(start)) {
            _endPicker.setErrorMessage("End must be after start");
            _endPicker.setInvalid(true);
        }
        _okButton.setEnabled(! (_startPicker.isInvalid() || _endPicker.isInvalid()));
    }


    private LocalDateTime longToDateTime(long msecs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(msecs), ZoneId.systemDefault());
    }

}
