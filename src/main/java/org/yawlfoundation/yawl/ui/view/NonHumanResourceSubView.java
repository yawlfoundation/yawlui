package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.resourcing.resource.nonhuman.NonHumanResource;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.NonHumanResourceDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public class NonHumanResourceSubView extends AbstractGridView<NonHumanResource> {

    public NonHumanResourceSubView(ResourceClient resClient) {
        super(resClient, null);
        build();
    }


    @Override
    List<NonHumanResource> getItems() {
        try {
            return getResourceClient().getNonHumanResources();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn(
                    "Failed to retrieve list of Non-human resources from engine : %s",
                    e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    String getTitle() {
        return "";
    }


    @Override
    protected boolean showHeader() {
        return false;
    }


    @Override
    void addColumns(Grid<NonHumanResource> grid) {
        grid.addColumn(NonHumanResource::getName).setHeader(UiUtil.bold("Name"));
        grid.addColumn(NonHumanResource::getDescription)
                .setHeader(UiUtil.bold("Description"));
        grid.addColumn(NonHumanResource::getCategoryName).setHeader(UiUtil.bold("Category"));
        grid.addColumn(NonHumanResource::getSubCategoryName)
                .setHeader(UiUtil.bold("Sub-category"));
    }


    @Override
    void configureComponentColumns(Grid<NonHumanResource> grid) {
        // no component cols
    }

    @Override
    void addItemActions(NonHumanResource item, ActionRibbon ribbon) {
        ActionIcon editIcon = ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
            NonHumanResourceDialog dialog = new NonHumanResourceDialog(
                    getResourceClient(), getLoadedItems(), item);
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    dialog.updateService();
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });

        // empty space to left to align with footer buttons
        editIcon.insertBlank();

        ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
            removeResource(item);
            ribbon.reset();
            refresh();
        });
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            NonHumanResourceDialog dialog = new NonHumanResourceDialog(
                    getResourceClient(), getLoadedItems(), null);
            dialog.getOkButton().addClickListener(e -> {
                if (dialog.validate()) {
                    dialog.updateService();
                    dialog.close();
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        }));

        ribbon.add(createMultiDeleteAction(
                event -> {
                    getGrid().getSelectedItems().forEach(this::removeResource);
                    ribbon.reset();
                    refresh();
                }));
        
        ribbon.add(createRefreshAction());
    }


    private void removeResource(NonHumanResource resource) {
        try {
            getResourceClient().removeNonHumanResource(resource);
        }
        catch (IOException e) {
            Announcement.error("Failed to remove non-human resource '%s': %s",
                    resource.getName(), e.getMessage());
        }
    }

}
