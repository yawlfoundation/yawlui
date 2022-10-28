/*
 * Copyright (c) 2004-2020 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.ui.dynform;


import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import org.yawlfoundation.yawl.documentStore.YDocument;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.upload.UploadDocumentDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.util.AddedIcons;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Michael Adams
 * @date 26/11/11
 */
public class DocComponent extends HorizontalLayout {
    
    private long _docID;
    private String _docName;
    private final String _caseID;
    private final TextField _textField;

    private ActionIcon _btnUp;
    private ActionIcon _btnDown;

    private final EngineClient _engClient;
    

    public DocComponent(EngineClient client, String caseID, String docIDStr,
                        TextField field, boolean inputOnly) {
        _engClient = client;
        _docID = StringUtil.strToLong(docIDStr, -1);
        _textField = field;
        _caseID = caseID;
        _docName = _textField.getValue();

        buildComponent(inputOnly);
    }

    
    public long getID() { return _docID; }

    
    public String getOutputXML() {
        StringBuilder sb = new StringBuilder();
        if (_docID > -1) sb.append(StringUtil.wrap(String.valueOf(_docID), "id"));
        sb.append(StringUtil.wrap(_docName, "name"));
        return sb.toString();
    }


    private void buildComponent(boolean inputOnly) {
        _textField.setReadOnly(true);                         // can't be edited directly

        String upTip = inputOnly ? "File is read only" : "Upload file";
        _btnUp = new ActionIcon(VaadinIcon.UPLOAD_ALT, null, upTip,
                e -> upload());
        _btnUp.setEnabled(! inputOnly);       // no upload if var readonly

        _btnDown = new ActionIcon(AddedIcons.DOWNLOAD, null, "Download File",
                e -> download());

        add(_textField, _btnUp, _btnDown);
        setSpacing(false);
    }


    private void setButtonToolTips() {
        String tip = _btnDown.isEnabled() ? "Download File" : "No file to download";
        _btnDown.setTooltip(tip);
        tip = _docID > -1 ? "Update the document" : "Upload a document";
        _btnUp.setTooltip(tip);
    }


    private void upload() {
        UploadDocumentDialog dialog = new UploadDocumentDialog();
        dialog.addSucceedListener(e -> {
            try {
                byte[] fileBytes = dialog.getFileContents(e.getFileName());
                if (fileBytes.length > 0) {
                    YDocument document = new YDocument(_caseID, _docID, fileBytes);
                    _docID = _engClient.putStoredDocument(document);
                    _docName = e.getFileName();
                    _textField.setValue(_docName);
                    _btnDown.setEnabled(true);
                    setButtonToolTips();
                }
                else {
                    Announcement.error("Failed to upload file: empty content");
                }
            }
            catch (IOException ex) {
                Announcement.error(ex.getMessage());
            }
        });
        dialog.open();
    }


    private void download() {
        try {
            YDocument doc = _engClient.getStoredDocument(_docID);
            if (doc.getDocument() != null) {
                downloadFile(_docName, doc.getDocument());
            }
            else Announcement.error("Unable to locate document: unknown document id.");
        }
        catch (IOException ioe) {
            Announcement.error("Unable to download document: " + ioe.getMessage());
        }
    }


    protected void downloadFile(String fileName, byte[] content) {
        InputStreamFactory isFactory = () -> new ByteArrayInputStream(content);
        StreamResource resource = new StreamResource(fileName, isFactory);
        resource.setContentType("multipart/form-data");
        resource.setCacheTime(0);
        resource.setHeader("Content-Disposition",
                "attachment;filename=\"" + fileName + "\"");

        Anchor downloadAnchor = new Anchor(resource, "");
        Element element = downloadAnchor.getElement();
        element.setAttribute("download", true);
        element.getStyle().set("display", "none");
        add(downloadAnchor);

        // simulate a click & remove anchor after file downloaded
        element.executeJs("return new Promise(resolve =>{this.click(); " +
                "setTimeout(() => resolve(true), 150)})", element)
                .then(jsonValue -> remove(downloadAnchor));
    }
    
}
