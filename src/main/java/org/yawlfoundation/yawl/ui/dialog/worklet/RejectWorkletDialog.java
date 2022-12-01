package org.yawlfoundation.yawl.ui.dialog.worklet;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * @author Michael Adams
 * @date 22/11/2022
 */
public class RejectWorkletDialog extends AbstractWorkletDialog {

    public RejectWorkletDialog(String caseID) {
        super("Reject Selected Worklet: Case " + caseID, null);
        setText("Please describe why the selected worklet is inappropriate for this case.");
    }


    protected VerticalLayout createContent(List<String> triggers) {
        return layoutTextFields();
    }

}
