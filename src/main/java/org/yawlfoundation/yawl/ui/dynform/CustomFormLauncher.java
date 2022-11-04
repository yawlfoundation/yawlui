package org.yawlfoundation.yawl.ui.dynform;

import com.vaadin.flow.component.UI;
import org.jdom2.Document;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.util.HttpURLValidator;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.SaxonUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.net.URL;

/**
 * @author Michael Adams
 * @date 31/10/2022
 */

public class CustomFormLauncher {

    private final String _userHandle;
    private URL _thisURL;

    public CustomFormLauncher(String handle) {
        _userHandle = handle;
        UI.getCurrent().getPage().fetchCurrentURL(this::storeURL);
    }


    public boolean show(WorkItemRecord wir, String caseData, String pid) {
        String url = wir.getCustomFormURL();
        if (!StringUtil.isNullOrEmpty(url)) {
            String uriPlusParams = buildURI(wir, caseData, pid);

            // check custom form exists and responds without error
            String validateMsg = HttpURLValidator.validate(uriPlusParams);
            if (validateMsg.equals("<success/>")) {
                UI.getCurrent().getPage().open(uriPlusParams);
                return true;
            }
        }
        return false;
    }


    private void storeURL(URL url) {
        _thisURL = url;
    }


    private String buildURI(WorkItemRecord wir, String caseData, String pid) {
        StringBuilder redir = new StringBuilder(parseCustomFormURI(wir, caseData));
        redir.append((redir.indexOf("?") == -1) ? "?" : "&")      // any static params?
                .append("workitem=").append(wir.getID())
                .append("&participantid=").append(pid)
                .append("&handle=").append(_userHandle);
        if (_thisURL != null) {
            redir.append("&callback=").append(buildCallbackURL(wir.getID()));
        }
        return redir.toString();
    }


    private String buildCallbackURL(String wirID) {
        return _thisURL.toExternalForm() + "customform/" +_userHandle + '/' + wirID + '/';
    }


    private String parseCustomFormURI(WorkItemRecord wir, String caseData) {
        String formURI = wir.getCustomFormURL();
        if (formURI.contains("{") && caseData != null) {
            Document dataDoc = JDOMUtil.stringToDocument(caseData);
            String[] parts = formURI.split("(?=\\{)|(?<=\\})");
            for (int i=0; i < parts.length; i++) {
                String part = parts[i];
                if (part.startsWith("{")) {
                    parts[i] = evaluateXQuery(
                            part.substring(1, part.lastIndexOf('}')), dataDoc);
                }
            }
            StringBuilder joined = new StringBuilder();
            for (String part : parts) {
                joined.append(part);
            }
            return joined.toString();
        }
        return formURI;
    }


    protected String evaluateXQuery(String s, Document dataDoc) {
        try {
            return SaxonUtil.evaluateQuery(s, dataDoc);
        }
        catch (Exception e) {
            return "__evaluation_error__";
        }
    }
    
}
