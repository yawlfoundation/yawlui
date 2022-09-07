package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.engine.interfce.SpecificationData;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.DelayedStartDialog;
import org.yawlfoundation.yawl.ui.dialog.SpecInfoDialog;
import org.yawlfoundation.yawl.ui.dialog.UploadDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.ui.view.AbstractGridView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/11/20
 */
public class SpecificationsSubView extends AbstractGridView<SpecificationData> {

    public SpecificationsSubView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
        build();
    }


    @Override
    List<SpecificationData> getItems() {
        try {
             return getResourceClient().getLoadedSpecificationData();
         }
         catch (IOException ioe) {
             announceError(ioe.getMessage());
             return Collections.emptyList();
         }
    }


    @Override
    void addColumns(Grid<SpecificationData> grid) {
        grid.addColumn(SpecificationData::getSpecURI).setHeader(
                UiUtil.bold("Name"));
        grid.addColumn(SpecificationData::getSpecVersion).setHeader(
                UiUtil.bold("Version"));
        grid.addColumn(SpecificationData::getDocumentation).setHeader(
                UiUtil.bold("Description"));
    }


    @Override
    void configureComponentColumns(Grid<SpecificationData> grid) { }    // no cols to configure


    @Override
    void addItemActions(SpecificationData item, ActionRibbon ribbon) {
        ribbon.add(VaadinIcon.CARET_RIGHT, ActionIcon.GREEN, "Start", event ->
            launchCase(item));
        ribbon.add(VaadinIcon.CLOCK, ActionIcon.GREEN, "Start later", event -> {
            delayedStart(item);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.INFO, "Info", event -> {
            showInfo(item);
            ribbon.reset();
        });
        ribbon.add(VaadinIcon.DOWNLOAD_ALT, "Download log", event -> {
            downloadLog(item);
            ribbon.reset();
         });
        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload", event -> {
            if (unloadSpecification(item)) {
                refresh();
            }
        });
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            new UploadDialog(getEngineClient(), getLoadedItems(), e -> refresh()).open();
            ribbon.reset();
        }));

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Unload Selected",
                event -> {
                    getGrid().getSelectedItems().forEach(this::unloadSpecification);
                    refresh();
                });

        ribbon.add(createRefreshAction());
    }


    @Override
    String getTitle() {
        return "Specifications";
    }


    private boolean unloadSpecification(SpecificationData spec) {
        boolean success = unloadSpecification(spec.getID());
        if (success) {
             Announcement.success("Specification " +
                     spec.getID().toString() + " unloaded");
        }
        return success;
    }


    private boolean launchCase(SpecificationData spec) {
        try {
            String caseID = getEngineClient().launchCase(spec.getID(), null);
            Announcement.success("Case " + caseID + " cancelled");
            return true;
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
            return false;
        }
    }


    private boolean unloadSpecification(YSpecificationID specID) {
        try {
            return getEngineClient().unloadSpecification(specID);
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
            return false;
        }
    }


    private void showInfo(SpecificationData specData) {
        new SpecInfoDialog(specData).open();
    }


    private void downloadLog(SpecificationData specData) {
        try {
            String log = getResourceClient().getMergedXESLog(
                    specData.getID(), true);
            if (log.isEmpty()) {
                Announcement.highlight("No cases for selected specification");
                return;
            }

            String fileName = String.format("%s_v%s.xes", specData.getSpecURI(),
                                        specData.getSpecVersion());
            InputStreamFactory isFactory = () -> new ByteArrayInputStream(
                    log.getBytes(StandardCharsets.UTF_8));
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
                    .then(jsonValue -> {
                        remove(downloadAnchor);
                        Announcement.success("XES log downloaded");
                    });

        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
        }
    }


    private void delayedStart(SpecificationData spec) {
        DelayedStartDialog dialog = new DelayedStartDialog();
        dialog.addOkClickListener(e -> {
            long delay = dialog.getDelay();
            if (delay > -1) {
                try {
                    getEngineClient().launchCase(spec.getID(), null, delay);
                }
                catch (IOException ioe) {
                    announceError(ioe.getMessage());
                }
                dialog.close();
            }
        });
        dialog.open();
    }

}
