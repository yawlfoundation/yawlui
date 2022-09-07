package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.ParticipantDetailsDialog;
import org.yawlfoundation.yawl.ui.dialog.YesNoDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;
import org.yawlfoundation.yawl.ui.service.ResourceClient;
import org.yawlfoundation.yawl.ui.util.UiUtil;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 25/8/2022
 */
public class ParticipantsView extends AbstractGridView<Participant> {

    private final boolean _isOrgDataModifiable;
    private Grid.Column<Participant> _isAdminColumn;

    public ParticipantsView(ResourceClient client) {
        super(client, null);
        _isOrgDataModifiable = client.isOrgDataModifiable();
        build();
    }


    @Override
    List<Participant> getItems() {
        return getParticipants();
    }


    @Override
    void addColumns(Grid<Participant> grid) {
        grid.addColumn(Participant::getUserID).setHeader(UiUtil.bold("User ID"));
        grid.addColumn(Participant::getLastName).setHeader(UiUtil.bold("Last Name"));
        grid.addColumn(Participant::getFirstName).setHeader(UiUtil.bold("First Name"));

        _isAdminColumn = grid.addComponentColumn(this::renderAdminColumn)
                .setHeader(UiUtil.bold("Admin"));

        grid.addColumn(Participant::getNotes).setHeader(UiUtil.bold("Notes"));
    }


    @Override
    void configureComponentColumns(Grid<Participant> grid) {
        _isAdminColumn.setAutoWidth(true).setFlexGrow(0).setResizable(false);
    }


    @Override
    void addItemActions(Participant item, ActionRibbon ribbon) {
        if (_isOrgDataModifiable) {
            ActionIcon editIcon = ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
                ParticipantDetailsDialog dialog = new ParticipantDetailsDialog(
                        getResourceClient(), getLoadedItems(), item);
                dialog.getOKButton().addClickListener(e -> dialogOkEvent(dialog));
                dialog.open();
                ribbon.reset();
            });

            // empty space to left to align with footer buttons
            editIcon.insertBlank();

            ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
                removeParticipant(item);
                ribbon.reset();
                refresh();
            });
        }
        else {
            ribbon.add(VaadinIcon.GLASSES, "View", event -> {
                new ParticipantDetailsDialog(getResourceClient(), item).open();
                ribbon.reset();
            });

        }
    }


    @Override
    void addFooterActions(ActionRibbon ribbon) {
        if (_isOrgDataModifiable) {
            ribbon.add(createAddAction(event -> {
                ParticipantDetailsDialog dialog = new ParticipantDetailsDialog(
                        getResourceClient(), getLoadedItems());
                dialog.getOKButton().addClickListener(e -> dialogOkEvent(dialog));
                dialog.open();
                ribbon.reset();
            }));

            ribbon.add(createMultiDeleteAction(
                    event -> {
                        removeParticipants(getGrid().getSelectedItems());
                        ribbon.reset();
                        refresh();
                    }));
        }

        ribbon.add(createRefreshAction());
    }


    @Override
    String getTitle() {
        return "Participants";
    }


    private Checkbox renderAdminColumn(Participant p) {
        Checkbox isAdmin = new Checkbox(p.isAdministrator());
        isAdmin.setReadOnly(true);
        return isAdmin;
    }


    private void dialogOkEvent(ParticipantDetailsDialog dialog) {
        if (dialog.validate()) {
            dialog.updateService();
            dialog.close();
            refresh();
        }
    }


    private List<Participant> getParticipants() {
        try {
            return getResourceClient().getParticipants();
        }
        catch (IOException | ResourceGatewayException e) {
            Announcement.error(e.getMessage());
            return Collections.emptyList();
        }
    }


    private void removeParticipant(Participant p) {
        YesNoDialog dialog = new YesNoDialog("Delete Participant",
                "Are you sure you want to delete participant " + p.getFullName() + "?");
        dialog.getYesButton().addClickListener(event -> {
            deleteParticipant(p);
            refresh();
        });
        dialog.open();
    }

    
    private void removeParticipants(Set<Participant> pSet) {
        YesNoDialog dialog = new YesNoDialog("Delete Participants",
                "Are you sure you want to delete " + pSet.size() +
                        " selected participants?");
        dialog.getYesButton().addClickListener(event -> {
            pSet.forEach(this::deleteParticipant);
            refresh();
        });
        dialog.open();
    }


    private void deleteParticipant(Participant p) {
        try {
            String result = getResourceClient().deleteParticipant(p);
            if (getResourceClient().successful(result)) {
                Announcement.success("Deleted participant '%s'",
                        p.getFullName());
            }
            else {
                Announcement.warn(StringUtil.unwrap(result));
            }
        }
       catch (IOException e) {
           Announcement.error(e.getMessage());
       }
    }

}
