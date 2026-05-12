package org.yawlfoundation.yawl.ui.component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.streams.DownloadHandler;
import io.netty.util.internal.StringUtil;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 *
 * @author Michael Adams
 * @date 28/4/2026
 */


public class DownloadIconWrapper extends Anchor {
    
    // string content
    public DownloadIconWrapper(ActionIcon icon,
                               Supplier<String> fileNameSupplier,
                               Supplier<String> contentSupplier,
                               Runnable postDownloadAction) {

        DownloadHandler handler = event -> {
            String content = contentSupplier.get();
            String fileName = fileNameSupplier.get();
            if (StringUtil.isNullOrEmpty(content)) {
                Announcement.error("Error downloading %s: no content",
                        fileName);
                return;
            }
            event.setFileName(fileName);
            event.setContentType("text/xml");

            try (OutputStream os = event.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
            }
            catch (Exception e) {
                Announcement.error("Error downloading %s: %s",
                        fileName, e.getMessage());
            }
        };
        
        configure(icon, handler, postDownloadAction);
    }


    // binary content
    public DownloadIconWrapper(ActionIcon icon,
                               Supplier<String> fileNameSupplier,
                               Supplier<byte[]> contentSupplier,
                               Runnable postDownloadAction,
                               boolean isBinary) {

        DownloadHandler handler = event -> {
            byte[] content = contentSupplier.get();
            String fileName = fileNameSupplier.get();
            if (content == null || content.length == 0) {
                Announcement.error("Error downloading %s: no content",
                        fileName);
                return;
            }
            event.setFileName(fileName);
            event.setContentType("application/octet-stream");

            try (OutputStream os = event.getOutputStream()) {
                os.write(content);
            }
            catch (Exception e) {
                Announcement.error("Error downloading %s: %s",
                        fileName, e.getMessage());
            }
        };

        configure(icon, handler, postDownloadAction);
    }


    private void configure(ActionIcon icon, DownloadHandler handler, Runnable postDownloadAction) {

        // Initialize the Anchor with the handler
        setHref(handler);
        getElement().setAttribute("download", true);

        // This JS triggers the download AND notifies Java to run post download logic
        getElement().addEventListener("click", e -> {
            if (postDownloadAction != null) {
                postDownloadAction.run();
            }
        });

        // Add the icon as the clickable face of the anchor
        add(icon);

        // 'hide' the anchor from alignment styling
        this.getStyle().set("display", "contents");
        
        // Style it to look like a clickable icon
        this.getStyle().set("cursor", "pointer");


    }
    
}

