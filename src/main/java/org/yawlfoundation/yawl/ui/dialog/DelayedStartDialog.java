package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import org.yawlfoundation.yawl.util.StringUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author Michael Adams
 * @date 1/8/2022
 */
public class DelayedStartDialog extends AbstractDialog {

    private final NumberField numberField = new NumberField("Seconds Delay:");
    private final TextField durationField = new TextField("After Duration:");
    private final DateTimePicker dateField = new DateTimePicker("At Exactly:");
    private final Div errDiv = new Div();
    private final Button ok = new Button("OK");

    private Component focussedField;

    public DelayedStartDialog() {
        super("Delayed Case Start");
        addComponent(createInputPanel());
        createButtons(ok);
    }


    public void addOkClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
        ok.addClickListener(listener);
    }


    // called from CasesSubView when OK is clicked
    public long getDelay() {
        long msecs = -1;
        if (focussedField != null) {
            if (focussedField instanceof NumberField) {
                msecs = numberField.getValue().longValue() * 1000;
            }
            else if (focussedField instanceof TextField) {
                String value = durationField.getValue();
                if (validateDuration(value)) {
                    msecs = Duration.parse(value).getSeconds() * 1000;
                }
            }
            else {
                msecs = dateField.getValue().atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli() - System.currentTimeMillis();
            }
        }
        return msecs;
    }



    private FormLayout createInputPanel() {
        FormLayout panel = new FormLayout();
        configNumberField();
        configDurationField();
        configDateField();
        configErrorDiv();
        panel.add(numberField, durationField, dateField, errDiv);
        panel.setColspan(dateField, 2);       
        panel.setColspan(errDiv, 2);
        return panel;
    }


    private void configNumberField() {
        numberField.setClearButtonVisible(true);
        numberField.setMax(Long.MAX_VALUE / 1000D);
        numberField.setMin(Long.MIN_VALUE / 1000D);
        numberField.addFocusListener(e -> focussedField = e.getSource());
        numberField.focus();
    }


    private void configDurationField() {
        durationField.setPattern("^P(?!$)(\\d+(?:\\.\\d+)?Y)?(\\d+(?:\\.\\d+)?M)?(\\d+" +
                "(?:\\.\\d+)?W)?(\\d+(?:\\.\\d+)?D)?(T(?=\\d)(\\d+(?:\\.\\d+)?H)?(\\d+" +
                "(?:\\.\\d+)?M)?(\\d+(?:\\.\\d+)?S)?)?$");
        durationField.setHelperText("Format: PnYnMnDTnHnMnS");
        durationField.setClearButtonVisible(true);
        durationField.addFocusListener(e -> focussedField = e.getSource());
    }


    private void configDateField() {
        dateField.setStep(Duration.ofSeconds(1));
        dateField.setValue(LocalDateTime.now());

        // the following has no effect in Vaadin 23 - left for documentation
//        dateField.addFocusListener(e -> focussedField = e.getSource());

        // this workaround sets a focus listener for each subcomponent
        dateField.getChildren().forEach(f -> ((FocusNotifier) f).addFocusListener(e ->
                focussedField = dateField));
    }


    private void configErrorDiv() {
        errDiv.getElement().getStyle().set("color", "red");
    }


    private boolean validateDuration(String value) {
        if (value.isEmpty() || StringUtil.isValidDurationString(value)) {
            errDiv.setText("");
            return true;
        }
        else {
            errDiv.setText("Invalid Duration value");
            return false;
        }
    }
    
}
