package org.yawlfoundation.yawl.ui.dialog.upload;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import org.apache.commons.io.IOUtils;
import org.yawlfoundation.yawl.ui.dialog.AbstractDialog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Michael Adams
 * @date 11/12/20
 */
public abstract class AbstractUploadDialog extends AbstractDialog {

    private final TextArea _msgArea = new TextArea("Messages");

    protected MultiFileMemoryBuffer buffer;
    private Upload upload;
    private Button closeBtn;


    public AbstractUploadDialog(String title, String... fileTypes) {
        super();
        createContent(title, fileTypes);
    }


    public void addSucceedListener(ComponentEventListener<SucceededEvent> listener) {
        upload.addSucceededListener(listener);
    }


    public void addCloseButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        closeBtn.addClickListener(listener);
    }


    protected String readFile(InputStream is) throws IOException {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }


    protected void appendMessage(String msg) {
        String existing = _msgArea.getValue();
        if (! existing.isEmpty()) {
            existing = existing + '\n';
        }
        _msgArea.setValue(existing + msg);
    }


    private void createContent(String title, String... fileTypes) {
        setHeader(title);
        addComponent(createUpload(fileTypes), createMsgArea());
        createButton();
    }


    private Upload createUpload(String... fileTypes) {
        buffer = new MultiFileMemoryBuffer();
        upload = new Upload(buffer);
        if (fileTypes != null) {
            upload.setAcceptedFileTypes(fileTypes);
        }
        upload.setWidthFull();
        return upload;
    }


    private TextArea createMsgArea() {
        _msgArea.setReadOnly(true);
        _msgArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        _msgArea.setWidthFull();
        return _msgArea;
    }

    
    private void createButton() {
        closeBtn = new Button("Close", event -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getButtonBar().add(closeBtn);
    }

}
