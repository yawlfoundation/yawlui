package org.yawlfoundation.yawl.ui.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
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
import java.util.List;

/**
 * @author Michael Adams
 * @date 11/12/20
 */
public class UploadSpecificationDialog extends AbstractUploadDialog {

    private final EngineClient _client;
    private final List<SpecificationData> _loadedSpecs;


    public UploadSpecificationDialog(EngineClient client, List<SpecificationData> loadedSpecs,
                                     ComponentEventListener<ClickEvent<Button>> listener) {
        super("Upload Specifications", ".yawl", ".xml");
        _client = client;
        _loadedSpecs = loadedSpecs;
        addCloseButtonListener(listener);
        addSucceedListener(event -> {
            String fileName = event.getFileName();
            uploadSpecification(fileName, buffer.getInputStream(fileName));
        });
    }


    private void uploadSpecification(String fileName, InputStream stream) {
         try {
             String fileContent = validateUpload(readFile(stream));
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


    private String unwrap(String s) {
        if (s == null) return s;
        while (s.trim().startsWith("<")) {
            s = StringUtil.unwrap(s);
        }
        return s;
    }
    
}
