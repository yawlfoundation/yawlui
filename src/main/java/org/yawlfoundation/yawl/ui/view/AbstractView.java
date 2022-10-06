package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Michael Adams
 * @date 7/9/2022
 */
public abstract class AbstractView extends VerticalLayout {

    private final ResourceClient _resClient;
    private final EngineClient _engClient;


    public AbstractView(ResourceClient resClient, EngineClient engClient) {
        _resClient = resClient;
        _engClient = engClient;
    }


    abstract Component createLayout();

    
    protected ResourceClient getResourceClient() { return _resClient; }

    protected EngineClient getEngineClient() { return _engClient; }


    protected SplitLayout createSplitView(Component top, Component bottom) {
        SplitLayout splitLayout = new SplitLayout(top, bottom);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
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
        Announcement.error(StringUtil.unwrap(msg));
    }

    
    protected boolean successful(String xmlMsg) {
        return getResourceClient().successful(xmlMsg);
    }


    protected void downloadFile(String fileName, String content) {
        InputStreamFactory isFactory = () -> new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));
        StreamResource resource = new StreamResource(fileName, isFactory);
        resource.setContentType("text/xml");
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
