package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.server.streams.DownloadHandler;
import org.jdom2.Document;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.*;
import org.yawlfoundation.yawl.ui.util.TNode;
import org.yawlfoundation.yawl.ui.util.TNodeParser;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.JDOMUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */

public abstract class AbstractView extends VerticalLayout {


    public AbstractView() { }

    abstract Component createLayout();


    protected ResourceClient getResourceClient() {
        return Clients.getResourceClient();
    }

    protected EngineClient getEngineClient() {
        return Clients.getEngineClient();
    }

    protected WorkletClient getWorkletClient() {
        return Clients.getWorkletClient();
    }

    protected DocStoreClient getDocStoreClient() {
        return Clients.getDocStoreClient();
    }

    protected LogClient getLogClient() {
        return Clients.getLogClient();
    }


    protected SplitLayout createSplitView(Component top, Component bottom) {
        return createSplitView(top, bottom, SplitLayout.Orientation.VERTICAL);
    }

    
    protected SplitLayout createSplitView(Component top, Component bottom,
                                          SplitLayout.Orientation orientation) {
        SplitLayout splitLayout = new SplitLayout(top, bottom);
        splitLayout.setOrientation(orientation);
        splitLayout.setSizeFull();
        splitLayout.addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        return splitLayout;
    }


    public H4 createHeader(String title) {
        H4 header = new H4(title);
        UiUtil.removeTopMargin(header);
        UiUtil.setStyle(header, "margin-bottom", "20px");
        return header;
    }


    protected void refreshHeader(H4 header, String text, int count) {
        header.getElement().setText(String.format("%s (%d)", text, count));
    }


    protected void announceError(String msg) {
        Announcement.error(msg);
    }

    
    protected boolean successful(String xmlMsg) {
        return getResourceClient().successful(xmlMsg);
    }


    protected void downloadFile(String fileName, String content) {
        DownloadHandler handler = event -> {
            event.setFileName(fileName);
            event.setContentType("text/xml");

            // Write the content to the output stream
            try (OutputStream os = event.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Handle IO error (logging, etc.)
            }
        };
    }


    protected TNode getInternalTypeTree(YSpecificationID specID) throws IOException {
        String specSchema = getEngineClient().getSpecificationDataSchema(specID);
        if (specSchema == null || ! specSchema.contains("yawl:")) {  // short circuit
            return null;
        }
        Document doc = JDOMUtil.stringToDocument(specSchema);
        TNode node = new TNodeParser().parse(doc.getRootElement());
        return node.hasInternalTypeInTree() ? node : null;
    }

}
