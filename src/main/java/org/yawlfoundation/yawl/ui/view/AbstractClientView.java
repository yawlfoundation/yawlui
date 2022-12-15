package org.yawlfoundation.yawl.ui.view;

import org.apache.commons.lang3.StringUtils;
import org.yawlfoundation.yawl.authentication.YClient;
import org.yawlfoundation.yawl.ui.announce.Announcement;
import org.yawlfoundation.yawl.ui.dialog.ClientDetailsDialog;
import org.yawlfoundation.yawl.ui.menu.ActionIcon;
import org.yawlfoundation.yawl.ui.menu.ActionRibbon;

import java.io.IOException;

/**
 * @author Michael Adams
 * @date 5/9/2022
 */
public abstract class AbstractClientView<T extends YClient> extends AbstractGridView<T> {

    public AbstractClientView() {
        super();
        build();
    }


    protected void addItemActions(T item, ActionRibbon ribbon) {
        ActionIcon editIcon = createEditAction(event -> {
            ClientDetailsDialog<T> dialog = new ClientDetailsDialog<>(getLoadedItems(), item);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    updateClient(item, dialog.composeClient());
                    dialog.close();
                    announceSuccess(item.getUserName(), "updated");
                    refresh();
                }
            });
            dialog.open();
            ribbon.reset();
        });
        editIcon.insertBlank();
        ribbon.add(editIcon);

        ribbon.add(createDeleteAction(event -> {
            if (removeClient(item)) {
                announceSuccess(item.getUserName(), "removed");
                refresh();
            }
        }));
    }

    
    protected void addFooterActions(ActionRibbon ribbon) {
        ribbon.add(createAddAction(event -> {
            ClientDetailsDialog<T> dialog = new ClientDetailsDialog<>(
                    getLoadedItems(), null);
            dialog.getSaveButton().addClickListener(e -> {
                if (dialog.validate()) {
                    YClient client = dialog.composeClient();
                    if (addClient(client)) {
                        refresh();
                        dialog.close();
                        announceSuccess(client.getUserName(), "added");
                    }
                }
            });
            dialog.open();
            ribbon.reset();
        }));

        ribbon.add(createMultiDeleteAction(
                event -> {
                    getGrid().getSelectedItems().forEach(item -> {
                        if (removeClient(item)) {
                            announceSuccess(item.getUserName(), "removed");
                        }
                    });
                    refresh();
                }));

        ribbon.add(createRefreshAction());
    }


    protected void announceSuccess(String clientName, String verb) {
        Announcement.success("%s %s %s",
                StringUtils.chop(getTitle()), clientName, verb);
    }


    protected boolean addClient(YClient client) {
        try {
            getResourceClient().addClient(client);
            return true;
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
            return false;
        }
    }


    protected void updateClient(T oldClient, YClient newClient) {
        removeClient(oldClient);
        addClient(newClient);
    }


    protected boolean removeClient(T client) {
        try {
            getResourceClient().removeClient(client);
            return true;
        }
        catch (IOException ioe) {
            announceError(ioe.getMessage());
            return false;
        }
    }

    

}
