package org.yawlfoundation.yawl.ui.dialog.upload;

import org.jdom2.Element;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Michael Adams
 * @date 21/9/2022
 */
public class UploadOrgDataDialog extends AbstractUploadDialog {

    public UploadOrgDataDialog(ResourceClient client) {
        super("Upload Org Data", ".ybkp");
        addSucceedListener(event -> {
            String fileName = event.getFileName();
            uploadOrgData(client, buffer.getInputStream(fileName));
        });
    }


    private void uploadOrgData(ResourceClient client, InputStream is) {
        try {
            String content = readFile(is);
            String outcome = client.importOrgData(content);
            if (StringUtil.isNullOrEmpty(outcome)) {
                appendMessage("Data import failed: see log file for details");
            }
            else {
                announceOutcome(outcome);
            }
        }
        catch (IOException e) {
            appendMessage("Data import failed: " + e.getMessage());
        }
    }


    private void announceOutcome(String outcome) {
        Element root = JDOMUtil.stringToElement(outcome);
        if (root == null || root.getChildren().isEmpty()) {
            appendMessage("Data import failed: malformed XML in .ybkp file");
        }
        else if (root.getContentSize() == 1) {
            appendMessage("Data import failed: " + root.getChildText("item"));
        }
        else {
            root.getChildren().forEach(item -> appendMessage(item.getText()));
        }
    }

}
