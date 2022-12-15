package org.yawlfoundation.yawl.ui.dialog.worklet;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * @author Michael Adams
 * @date 22/11/2022
 */
public class RejectWorkletDialog extends AbstractWorkletDialog {

    public RejectWorkletDialog(String caseID) {
        super("Reject Selected Worklet: Case " + caseID, null);
    }


    protected VerticalLayout createContent(List<String> triggers) {
        VerticalLayout layout = layoutTextFields();
        String text = "Describe why the selected worklet is inappropriate for this case.";
        layout.addComponentAsFirst(new Span(text));
        return layout;
    }

}
