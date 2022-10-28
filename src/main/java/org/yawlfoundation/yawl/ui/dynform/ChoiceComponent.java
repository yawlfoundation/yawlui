package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 24/10/2022
 */
public class ChoiceComponent extends Accordion {

    public static final double SUMMARY_HEIGHT = 32.0;
    public static final double SPACING = 22.0;

    private SubPanel chosenPanel = null;

    // We want to initially have open the choice represented by the input data, but the
    // UI will always override the initial selection and open the first panel when the
    // form is displayed. This boolean is used to block the UI's initial choice.
    private boolean initialised = false;

    public ChoiceComponent(List<Component> choices, String inputValue) {
        for (Component choice : choices) {
            ChoiceHeader header = createHeader(choice);
            AccordionPanel panel = new AccordionPanel(header, choice);
            panel.addThemeVariants(DetailsVariant.FILLED);
            panel.addOpenedChangeListener(event -> {
               if (event.isOpened()) {
                   if (initialised) {
                       unselectAll();
                       ((ChoiceHeader) panel.getSummary()).setSelected(event.isOpened());
                       panel.getContent().findFirst().ifPresent(c ->
                               chosenPanel = (SubPanel) c);
                   }
                   else {
                       initialised = true;
                       makeInitialSelection(inputValue);
                   }
                }
            });
            add(panel);
        }
    }


    private void makeInitialSelection(String value) {
        boolean selected = false;
        if (value != null) {
            XNode dataNode = new XNodeParser().parse(value);
            if (dataNode != null) {
                XNode child = dataNode.getChild(0);
                if (child != null) {
                    selectByName(child.getName());
                    selected = true;
                }
            }
        }

        // if no selection made, default to first choice
        if (! selected) {
            getChildren().findFirst().ifPresent(
                    first -> ((AccordionPanel) first).setOpened(true));
        }
    }


    private void selectByName(String name) {
        getChildren().forEach(c -> {
            AccordionPanel panel = (AccordionPanel) c;
            ChoiceHeader header = (ChoiceHeader) panel.getSummary();
            if (header.title.equals(name) && ! panel.isOpened()) {
                panel.setOpened(true);
            }
        });
    }


    public SubPanel getChosenPanel() {
        return chosenPanel;
    }


    public List<Component> getContent() {
        List<Component> content = new ArrayList<>();
        getChildren().forEach(component ->
                content.addAll(((AccordionPanel) component).getContent()
                        .collect(Collectors.toList())));
        return content;
    }


    public double calculateHeight() {
        double summaryHeight = getChildren().count() * SUMMARY_HEIGHT;
        double maxFormHeight = 0;
        for (Component c : getChildren().collect(Collectors.toList())) {
            AccordionPanel panel = (AccordionPanel) c;
            Component content = panel.getContent().collect(Collectors.toList()).get(0);
            double contentHeight = 0;
            if (content instanceof SubPanel) {
                contentHeight = ((SubPanel) content).calculateHeight();
            }
            else if (content instanceof Checkbox) {
                contentHeight = DynFormLayout.CHECKBOX_HEIGHT;
            }
            else {
                contentHeight = DynFormLayout.FIELD_HEIGHT;
            }
            maxFormHeight = Math.max(maxFormHeight, contentHeight);
        }
        return SPACING + summaryHeight + maxFormHeight;
    }


    private void unselectAll() {
        getChildren().forEach(c -> {
            AccordionPanel panel = (AccordionPanel) c;
            ((ChoiceHeader) panel.getSummary()).setSelected(false);
        });
    }


    private ChoiceHeader createHeader(Component component) {
        String title;
        if (component instanceof SubPanel) {
            title = ((SubPanel) component).getName();
        }
        else {
            title = ((HasLabel)component).getLabel();
        }
        return new ChoiceHeader(title);
    }


    class ChoiceHeader extends HorizontalLayout {

        final Checkbox cb = new Checkbox();
        final String title;

        ChoiceHeader(String title) {
            this.title = title;
            Span span = new Span(title);
            span.getStyle().set("line-height", "inherit");
            add(span);
            cb.setEnabled(false);
            add(cb);
            setSelected(false);
            setMargin(false);
        }

        void setSelected(boolean select) {
            cb.setValue(select);
        }
    }

}
