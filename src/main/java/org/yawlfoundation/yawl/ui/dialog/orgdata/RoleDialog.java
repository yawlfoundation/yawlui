package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Role;
import org.yawlfoundation.yawl.ui.component.MultiSelectParticipantList;

import java.util.List;

/**
 * @author Michael Adams
 * @date 9/9/2022
 */
public class RoleDialog extends AbstractOrgDataDialog<Role> {

    private ComboBox<Role> _belongsCombo;
    private final List<Participant> _members;
    private final Role _belongsTo;


    public RoleDialog(List<Role> items, Role item, List<Participant> allParticipants,
                      List<Participant> members, Role belongsTo) {
        super(items, item, allParticipants, "Role");
        _members = members;
        _belongsTo = belongsTo;
        build();
        setWidth("700px");
 //       setHeight("350px");
    }

    @Override
    void addBelongsToCombo(FormLayout form, Role item) {
        _belongsCombo = new ComboBox<>("Belongs To");
        List<Role> allRoles = getExistingItems();
        Role nilRole = new Role("nil");
        allRoles.add(0, nilRole);
        _belongsCombo.setItems(allRoles);
        _belongsCombo.setItemLabelGenerator(Role::getName);
        if (isEditing() && _belongsTo != null) {
            _belongsCombo.setValue(_belongsTo);
        }
        else {
            _belongsCombo.setValue(nilRole);
        }
        form.add(_belongsCombo, 1);
    }

    @Override
    void addGroupCombo(FormLayout form, Role item) { }

    @Override
    MultiSelectParticipantList createMembersList(Role item) {
        MultiSelectParticipantList list = new MultiSelectParticipantList(
                getParticipantList(), null, false);
        list.setSelected(_members);
        list.setWidth("100%");
        return list;
    }

    @Override
    public Role compose() {
        return null;
    }
}
