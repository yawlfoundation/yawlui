package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.schema.YSchemaVersion;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.UploadResult;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Michael Adams
 * @date 11/12/20
 */
public class UploadDialog extends Dialog {

    private final EngineClient _client;
    private final List<SpecificationData> _loadedSpecs;

    private final TextArea _msgArea = new TextArea("Messages");

    public UploadDialog(EngineClient client, List<SpecificationData> loadedSpecs,
                        ComponentEventListener<ClickEvent<Button>> listener) {
        super();
        _client = client;
        _loadedSpecs = loadedSpecs;

        setCloseOnOutsideClick(false);
        setWidth("700px");
        setHeight("515px");
        add(getContent(listener));
    }


    private VerticalLayout getContent(ComponentEventListener<ClickEvent<Button>> listener) {
        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Upload Specifications"));
        layout.add(createUpload());
        layout.add(createMsgArea());
        layout.add(createButton(listener));
        layout.setSizeFull();
        return layout;
    }


    private Upload createUpload() {
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".yawl", ".xml");
        upload.setHeight("60%");
        upload.setWidthFull();

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            uploadSpecification(fileName, buffer.getInputStream(fileName));
        });
        return upload;
    }


    private TextArea createMsgArea() {
        _msgArea.setReadOnly(true);
        _msgArea.setHeight("30%");
        _msgArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        _msgArea.setWidthFull();
        return _msgArea;
    }

    
    private HorizontalLayout createButton(ComponentEventListener<ClickEvent<Button>> listener) {
        HorizontalLayout layout = new HorizontalLayout();
        Button b = new Button("Close", event -> {
            close();
        });
        b.addClickListener(listener);
        layout.add(b);
        return layout;
    }


    private void uploadSpecification(String fileName, InputStream stream) {
         try {
             String fileContent = validateUpload(IOUtils.toString(stream,
                     StandardCharsets.UTF_8));
             UploadResult result = _client.uploadSpecification(fileContent);
             processResult(result, fileName);
         }
         catch (IOException ioe) {
             appendMessage(formatMessage("ERROR", stripPath(fileName), unwrap(ioe.getMessage())));
         }
     }


    private String validateUpload(String content) throws IOException {
        int BOF = content.indexOf("<?xml");
        int EOF = content.indexOf("</specificationSet>");
        if (BOF != -1 && EOF != -1) {
            String trimmedContent = content.substring(BOF, EOF + 19);
            hasUniqueDescriptors(trimmedContent);                      // throws IOException
            return trimmedContent;
        }
        return content;
    }


    private void hasUniqueDescriptors(String specxml) throws IOException {
        if (StringUtil.isNullOrEmpty(specxml)) {
            throw new IOException("Invalid specification file: null or empty contents.");
        }
        YSpecificationID specID = getDescriptors(specxml);
        if (! specID.isValid()) {
            throw new IOException("Invalid specification: missing identifier or incorrect version.");
        }

        for (SpecificationData spec : _loadedSpecs) {
            if (spec.getID().equals(specID)) {
                if (specID.getUri().equals(spec.getSpecURI())) {
                    throw new IOException("This specification is already loaded.");
                }
                else {
                    throw new IOException("A specification with the same id and " +
                            "version (but different name) is already loaded.");
                }
            }
            else if (specID.isPreviousVersionOf(spec.getID())) {
                if (specID.getUri().equals(spec.getSpecURI())) {
                    throw new IOException("A later version of this specification is " +
                            "already loaded.");
                }
                else {
                    throw new IOException("A later version of a specification with the " +
                            "same id (but different name) is already loaded.");
                }
            }
            else if (specID.getUri().equals(spec.getSpecURI()) &&
                    (! specID.hasMatchingIdentifier(spec.getID()))) {
                throw new IOException("A specification with the same name, but a different " +
                        "id, is already loaded. Please change the name and try again.");
            }
        }
    }


    private YSpecificationID getDescriptors(String specxml) throws IOException {
        Document doc = JDOMUtil.stringToDocument(specxml);
        if (doc != null) {
            Element root = doc.getRootElement();
            Namespace ns = root.getNamespace();
            YSchemaVersion schemaVersion = YSchemaVersion.fromString(
                    root.getAttributeValue("version"));
            Element specification = root.getChild("specification", ns);

            if (specification != null) {
                String uri = specification.getAttributeValue("uri");
                String version = "0.1";
                String uid = null;
                if (! (schemaVersion == null || schemaVersion.isBetaVersion())) {
                    Element metadata = specification.getChild("metaData", ns);
                    version = metadata.getChildText("version", ns);
                    uid = metadata.getChildText("identifier", ns);
                }
                return new YSpecificationID(uid, version, uri);
            }
            else throw new IOException("Malformed specification: 'specification' node not found.");
        }
        else  throw new IOException("Malformed specification: unable to parse.");
    }


    // removes the path part from an absolute filename
    private String stripPath(String fileName) {
        return stripPath(stripPath(fileName, '/'), '\\');
    }


    private String stripPath(String fileName, char slash) {
        int index = fileName.lastIndexOf(slash);
        if (index >= 0) fileName = fileName.substring(index + 1);
        return fileName ;
    }


    private void processResult(UploadResult result, String fileName) {
        fileName = stripPath(fileName);

        for (String error : result.getErrors()) {
             appendMessage(formatMessage("ERROR", fileName, error));
        }
        for (String warning : result.getWarnings()) {
             appendMessage(formatMessage("WARNING", fileName, warning));
        }

        // add specids to loaded specs for consequent uniqueness checks
        if (result.hasSpecIDs()) {
            for (YSpecificationID specID : result.getSpecIDs()) {
                _loadedSpecs.add(new SpecificationData(specID, null,
                        null, null, null));
            }
            appendMessage("SUCCESS (" + fileName + ").\n\n");
        }
    }


    private String formatMessage(String label, String fileName, String msg) {
        return String.format("%s (%s): %s.\n\n", label, fileName, msg);
    }


    private void appendMessage(String msg) {
        String existing = _msgArea.getValue();
        _msgArea.setValue(msg + existing);
    }


    private String unwrap(String s) {
        if (s == null) return s;
        while (s.trim().startsWith("<")) {
            s = StringUtil.unwrap(s);
        }
        return s;
    }


}
