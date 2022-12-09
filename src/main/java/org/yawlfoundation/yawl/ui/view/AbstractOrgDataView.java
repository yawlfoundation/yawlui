package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.apache.commons.lang3.StringUtils;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.dialog.upload.UploadOrgDataDialog;
import org.yawlfoundation.yawl.ui.layout.JustifiedButtonLayout;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public abstract class AbstractOrgDataView<T extends AbstractResourceAttribute>
        extends AbstractGridView<T> {

    protected Grid.Column<T> _colBelongsTo;
    protected Grid.Column<T> _colMembers;


    protected AbstractOrgDataView() {
        super(false);
        build();
    }


    @Override
    void addColumns(Grid<T> grid) {
        grid.addColumn(AbstractResourceAttribute::getName)
                .setHeader(UiUtil.bold("Name"));
        grid.addColumn(AbstractResourceAttribute::getDescription)
                .setHeader(UiUtil.bold("Description"));
        grid.addColumn(AbstractResourceAttribute::getNotes)
                .setHeader(UiUtil.bold("Notes"));
        addBelongsToColumn(grid);

        if (hasMembers()) {
            _colMembers = grid.addComponentColumn(this::getMembersComponent)
                    .setHeader(UiUtil.bold("Members"));
        }
    }


    @Override
    void configureComponentColumns(Grid<T> grid) {
        if (_colBelongsTo != null) {
            _colBelongsTo.setAutoWidth(true).setFlexGrow(0).setResizable(false);
        }
        if (_colMembers != null) {
            _colMembers.setAutoWidth(true).setFlexGrow(0).setResizable(false);
        }
    }


    @Override
    protected void addItemActions(T item, ActionRibbon ribbon) {
        ActionIcon editIcon = createEditAction(event -> {
            AbstractOrgDataDialog<T> dialog = createDialog(getLoadedItems(), item);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    updateItem(dialog.compose());
                    updateMembers(dialog.getStartingMembers(), dialog.getUpdatedMembers(), item);
                    dialog.close();
                    announceSuccess(item.getName(), "updated");
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });
        editIcon.insertBlank();
        ribbon.add(editIcon);

        ribbon.add(createDeleteAction(event -> {
            if (removeItem(item)) {
                announceSuccess(item.getName(), "removed");
                refresh();
            }
        }));
    }

    
    @Override
    protected void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            AbstractOrgDataDialog<T> dialog = createDialog(getLoadedItems(), null);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    T item = dialog.compose();
                    item = addItem(item);
                    String id = item.getID();
                    if (id != null && dialog.hasUpdatedMembers()) {
                        addMembers(dialog.getUpdatedMembers(), item);
                    }
                    refresh();
                    dialog.close();
                    announceSuccess(item.getName(), "added");
                }
            });
            dialog.open();
            ribbon.reset();
        }));

        ribbon.add(createMultiDeleteAction(
                event -> {
                    getGrid().getSelectedItems().forEach(item -> {
                        if (removeItem(item)) {
                            announceSuccess(item.getName(), "removed");
                        }
                    });
                    refresh();
                }));

        ribbon.add(createRefreshAction());
    }


    @Override
    protected void addGridFooter(Grid<?> grid, Component... components) {
        super.addGridFooter(grid, components);

        // add upload and download buttons to 2nd last footer cell
        FooterRow footerRow = grid.getFooterRows().get(0);
        int lastColIndex = grid.getColumns().size() - 2;
        footerRow.getCell(grid.getColumns().get(lastColIndex)).setComponent(
                new JustifiedButtonLayout(createFileActions()));
    }

    abstract void addBelongsToColumn(Grid<T> grid);
    
    abstract List<Participant> getMembers(T item);

    abstract void addMembers(List<Participant> pList, T item);

    abstract void removeMembers(List<Participant> pList, T item);


    abstract T addItem(T item);

    abstract boolean updateItem(T item);

    abstract boolean removeItem(T item);

    abstract AbstractOrgDataDialog<T> createDialog(List<T> existingItems, T item);

    protected boolean hasMembers() { return true; }


    protected Component getMembersComponent(T item) {
        List<Participant> pList = getMembers(item);
        if (pList.size() > 1) {
            return buildParticipantCombo(pList);
        }

        if (pList.size() == 1) {
            return new Label(pList.get(0).getFullName());
        }
        else {
            return new Label();
        }
    }


    protected ActionRibbon createFileActions() {
        ActionRibbon ribbon = new ActionRibbon();
        ribbon.add(VaadinIcon.UPLOAD_ALT, "Upload from backup file", e -> {
            UploadOrgDataDialog dialog = new UploadOrgDataDialog();
            dialog.addCloseButtonListener(b -> {
                refresh();
                ribbon.reset();
            });
            dialog.open();
        });
        ribbon.add(VaadinIcon.DOWNLOAD_ALT, "Download to backup file", e -> {
            download();
            ribbon.reset();
        });
        return ribbon;
    }


    protected List<Participant> getAllParticipants() {
        try {
            return getResourceClient().getParticipants();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.warn("Failed to get list of Participants from engine : " +
                    e.getMessage());
        }
        return Collections.emptyList();
    }

    
    protected void announceSuccess(String clientName, String verb) {
        String object = StringUtils.chop(getTitle());
        if (object.endsWith("ie")) {
            object = object.replace("ie", "y");
        }
        Announcement.success("%s %s %s", object, clientName, verb);
    }


    protected void updateMembers(List<Participant> oldList,
                                 List<Participant> newList, T item) {
        removeMembers(notInOther(oldList, newList), item);
        addMembers(notInOther(newList, oldList), item);
    }


    protected void download() {
        try {
            String content = getResourceClient().exportOrgData();
            downloadFile("YAWLOrgDataExport.ybkp", content);
            Announcement.success("Org data successfully downloaded");
        }
        catch (IOException e) {
            Announcement.error("Failed to download org data: %s", e.getMessage());
        }
    }


    protected List<Participant> notInOther(List<Participant> master,
                                           List<Participant> other) {
        return master.stream().filter(
                        m -> other.stream().noneMatch(
                                o -> m.getID().equals(o.getID())))
                .collect(Collectors.toList());
    }

}
