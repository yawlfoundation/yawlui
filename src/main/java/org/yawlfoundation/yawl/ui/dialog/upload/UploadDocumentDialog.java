package org.yawlfoundation.yawl.ui.dialog.upload;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Called from DynForm's DocComponent
 * @author Michael Adams
 * @date 21/9/2022
 */
public class UploadDocumentDialog extends AbstractUploadDialog {

    public UploadDocumentDialog() {
        super("Upload File", (String) null);
    }


    public byte[] getFileContents(String fileName) throws IOException {
        return IOUtils.toByteArray(buffer.getInputStream(fileName));
    }

}
