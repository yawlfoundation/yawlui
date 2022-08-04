package org.yawlfoundation.yawl.ui.dialog;

/**
 * @author Michael Adams
 * @date 29/7/2022
 */
public class SimpleMessageDialog extends MessageDialog {

    public SimpleMessageDialog(String title, String msg) {
        super(title);
        setText(msg);
        addConfirmButton("OK");
    }
}
