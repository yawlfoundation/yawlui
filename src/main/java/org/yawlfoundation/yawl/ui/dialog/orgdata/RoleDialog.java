package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import org.yawlfoundation.yawl.resourcing.resource.Participant;
import org.yawlfoundation.yawl.resourcing.resource.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Adams
 * @date 9/9/2022
 */
public class RoleDialog extends AbstractOrgDataDialog<Role> {

    private ComboBox<Role> _belongsCombo;
    private final Role _belongsTo;


    public RoleDialog(List<Role> items, Role item, List<Participant> allParticipants,
                      List<Participant> members, Role belongsTo) {
        super(items, item, allParticipants, members, "Role");
        _belongsTo = belongsTo;
        build();
        setWidth("700px");
    }

    @Override
    void addBelongsToCombo(FormLayout form, Role item) {
        _belongsCombo = new ComboBox<>("Belongs To");
        List<Role> allRoles = new ArrayList<>(getExistingItems());
        Role nilRole = new Role("nil");
        nilRole.setID("RO-nil");
        initCombo(_belongsCombo, allRoles, _belongsTo, nilRole);
        form.add(_belongsCombo, 1);
    }

    
    @Override
    void addGroupCombo(FormLayout form, Role item) { }


    @Override
    public Role compose() {
        Role role = isEditing() ? getItem() : new Role();
        role.setName(getNameValue());
        role.setDescription(getDescriptionValue());
        role.setNotes(getNotesValue());

        Role belongsTo = _belongsCombo.getValue();
        if (belongsTo.getName().equals("nil")) {
            belongsTo = null;
        }
        role.setOwnerRole(belongsTo);

        return role;
    }


    @Override
    public boolean validate() {
        return super.validate() &
                validateCombo(_belongsCombo, "role", "belong") ;
    }

}
