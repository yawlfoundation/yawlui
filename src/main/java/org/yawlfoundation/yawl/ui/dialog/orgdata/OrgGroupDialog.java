package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import org.yawlfoundation.yawl.resourcing.resource.OrgGroup;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.List;

/**
 * @author Michael Adams
 * @date 9/9/2022
 */
public class OrgGroupDialog extends AbstractOrgDataDialog<OrgGroup> {

    private ComboBox<OrgGroup> _belongsCombo;
    private ComboBox<OrgGroup.GroupType> _typeCombo;
    private final OrgGroup _belongsTo;


    public OrgGroupDialog(List<OrgGroup> items, OrgGroup item, List<Participant> allParticipants,
                          List<Participant> members, OrgGroup belongsTo) {
        super(items, item, allParticipants, members, "Org Group");
        _belongsTo = belongsTo;
        build();
        setWidth("450px");
    }


    @Override
    public String getMemberHeight() { return "290px"; }

    @Override
    public String getSelectMembersHeight() { return "257px"; }

    @Override
    void addBelongsToCombo(FormLayout form, OrgGroup item) {
        _belongsCombo = new ComboBox<>("Belongs To");
        List<OrgGroup> allGroups = getExistingItems();
        OrgGroup nilGroup = new OrgGroup();
        nilGroup.setLabel("nil");
        nilGroup.setID("OG-nil");
        initCombo(_belongsCombo, allGroups, _belongsTo, nilGroup);
        form.add(_belongsCombo, 1);
    }


    @Override
    void addGroupCombo(FormLayout form, OrgGroup item) {
        _typeCombo = new ComboBox<>("Group Type");
        _typeCombo.setItems(OrgGroup.GroupType.values());
        if (item != null) {
            _typeCombo.setValue(item.getGroupType());
        }
        else {
            _typeCombo.setValue(OrgGroup.GroupType.GROUP);
        }
        form.add(_typeCombo, 1);
    }


    @Override
    public OrgGroup compose() {
        OrgGroup group = isEditing() ? getItem() : new OrgGroup();
        group.setLabel(getNameValue());
        group.setDescription(getDescriptionValue());
        group.setNotes(getNotesValue());
        group.setGroupType(_typeCombo.getValue());

        OrgGroup belongsTo = _belongsCombo.getValue();
        if (belongsTo.getName().equals("nil")) {
            belongsTo = null;
        }
        group.setBelongsTo(belongsTo);

        return group;
    }


    @Override
    public boolean validate() {
        return super.validate() &
                validateCombo(_belongsCombo, "org group", "belong");
    }
}
