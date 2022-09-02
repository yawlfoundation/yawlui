package org.yawlfoundation.yawl.ui.view;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.rsInterface.ResourceGatewayException;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.ParticipantDetailsDialog;
import org.yawlfoundation.yawl.ui.dialog.YesNoDialog;
import org.yawlfoundation.yawl.ui.layout.UnpaddedVerticalLayout;
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
public class ParticipantsView extends AbstractView {

    private final boolean _isOrgDataModifiable;
    private List<Participant> _participants;
    private Grid<Participant> _grid;
    private H4 _header;


    public ParticipantsView(ResourceClient client) {
        super(client, null);
        _isOrgDataModifiable = client.isOrgDataModifiable();
        _participants = getParticipants();
        _header = new H4("Participants (" + _participants.size() + ")");
        add(createPanel());
        setSizeFull();
    }


    private UnpaddedVerticalLayout createPanel() {
        UnpaddedVerticalLayout gridPanel = createGridPanel(_header,createGrid());
        gridPanel.setSizeFull();
        return gridPanel;

    }

    private Grid<Participant> createGrid() {
        _grid = new Grid<>();
        _grid.setItems(_participants);
        _grid.setSelectionMode(Grid.SelectionMode.MULTI);
        _grid.addColumn(Participant::getUserID).setHeader(UiUtil.bold("User ID"));
        _grid.addColumn(Participant::getLastName).setHeader(UiUtil.bold("Last Name"));
        _grid.addColumn(Participant::getFirstName).setHeader(UiUtil.bold("First Name"));

        Grid.Column<Participant> isAdminColumn = _grid.addComponentColumn(this::renderAdminColumn);
        isAdminColumn.setHeader(UiUtil.bold("Admin"));

        _grid.addColumn(Participant::getNotes).setHeader(UiUtil.bold("Notes"));

        Grid.Column<Participant> actionColumn = _grid.addComponentColumn(this::renderActionColumn);

        configureGrid(_grid);
        configureActionColumn(actionColumn);
        isAdminColumn.setAutoWidth(true).setFlexGrow(0).setResizable(false);
        addGridFooter(_grid, createFooterActions());
        return _grid;
    }


    private Checkbox renderAdminColumn(Participant p) {
        Checkbox isAdmin = new Checkbox(p.isAdministrator());
        isAdmin.setReadOnly(true);
        return isAdmin;
    }


    private ActionRibbon renderActionColumn(Participant p) {
        ActionRibbon ribbon = new ActionRibbon();
        if (_isOrgDataModifiable) {
            ActionIcon editIcon = ribbon.add(VaadinIcon.PENCIL, "Edit", event -> {
                ParticipantDetailsDialog dialog = new ParticipantDetailsDialog(
                        _resClient, _participants, p);
                dialog.getOKButton().addClickListener(e -> dialogOkEvent(dialog));
                dialog.open();
                ribbon.reset();
            });

            // empty space to left to align with footer buttons
            editIcon.getStyle().set("margin-left", "38px");

            ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove", event -> {
                removeParticipant(p);
                ribbon.reset();
                refreshGrid();
            });
        }
        else {
            ribbon.add(VaadinIcon.GLASSES, "View", event -> {
                new ParticipantDetailsDialog(_resClient, p).open();
                ribbon.reset();
            });

        }

        return ribbon;
    }


    private ActionRibbon createFooterActions() {
        ActionRibbon ribbon = new ActionRibbon();
        if (_isOrgDataModifiable) {
            ribbon.add(VaadinIcon.PLUS, "Add", event -> {
                ParticipantDetailsDialog dialog = new ParticipantDetailsDialog(
                        _resClient, _participants);
                dialog.getOKButton().addClickListener(e -> dialogOkEvent(dialog));
                dialog.open();
                ribbon.reset();
            });

            ribbon.add(VaadinIcon.CLOSE_SMALL, ActionIcon.RED, "Remove Selected",
                    event -> {
                        removeParticipants(_grid.getSelectedItems());
                        ribbon.reset();
                        refreshGrid();
                    });
        }

        ribbon.add(VaadinIcon.REFRESH, "Refresh", event -> refreshGrid());

        return ribbon;
    }


    private void dialogOkEvent(ParticipantDetailsDialog dialog) {
        if (dialog.validate()) {
            dialog.updateService();
            dialog.close();
            refreshGrid();
        }
    }


    private void refreshGrid() {
        _participants = getParticipants();
        _grid.setItems(_participants);
        _grid.getDataProvider().refreshAll();
        _grid.recalculateColumnWidths();
        refreshHeader(_header, "Participants", _participants.size());
    }


    private List<Participant> getParticipants() {
        try {
            return _resClient.getParticipants();
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
            refreshGrid();
        });
        dialog.open();
    }

    
    private void removeParticipants(Set<Participant> pSet) {
        YesNoDialog dialog = new YesNoDialog("Delete Participants",
                "Are you sure you want to delete " + pSet.size() +
                        " selected participants?");
        dialog.getYesButton().addClickListener(event -> {
            pSet.forEach(this::deleteParticipant);
            refreshGrid();
        });
        dialog.open();
    }


    private void deleteParticipant(Participant p) {
        try {
            String result = _resClient.deleteParticipant(p);
            if (_resClient.successful(result)) {
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
