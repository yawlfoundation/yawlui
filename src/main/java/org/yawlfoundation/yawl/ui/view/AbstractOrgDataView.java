package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import org.apache.commons.lang3.StringUtils;
import org.yawlfoundation.yawl.resourcing.resource.AbstractResourceAttribute;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.orgdata.AbstractOrgDataDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.EngineClient;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael Adams
 * @date 8/9/2022
 */
public abstract class AbstractOrgDataView<T extends AbstractResourceAttribute>
        extends AbstractGridView<T> {

    protected Grid.Column<T> _colBelongsTo;
    protected Grid.Column<T> _colMembers;


    protected AbstractOrgDataView(ResourceClient resClient, EngineClient engClient) {
        super(resClient, engClient);
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
                    if (addItem(item)) {
                        refresh();
                        dialog.close();
                        announceSuccess(item.getName(), "added");
                    }
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
    protected boolean showHeader() {
        return false;
    }

    abstract void addBelongsToColumn(Grid<T> grid);
    
    abstract List<Participant> getMembers(T item);
    
    abstract boolean addItem(T item);

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
        Announcement.success("%s %s %s",
                StringUtils.chop(getTitle()), clientName, verb);
    }

}
