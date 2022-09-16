package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import org.yawlfoundation.yawl.resourcing.resource.*;

import java.util.List;

/**
 * @author Michael Adams
 * @date 9/9/2022
 */
public class PositionDialog extends AbstractOrgDataDialog<Position> {

    private ComboBox<Position> _reportsCombo;
    private final Position _reportsTo;

    private ComboBox<OrgGroup> _belongsCombo;
    private List<OrgGroup> _allGroups;
    private final OrgGroup _belongsTo;


    public PositionDialog(List<Position> items, Position item,
                          List<Participant> allParticipants,
                          List<Participant> members, Position reportsTo,
                          List<OrgGroup> allGroups, OrgGroup belongsTo) {
        super(items, item, allParticipants, members, "Position");
        _reportsTo = reportsTo;
        _allGroups = allGroups;
        _belongsTo = belongsTo;
        build();
        setWidth("700px");
    }

    // for positions, the 'belongs to' combo is actually the position this one reports to
    @Override
    void addBelongsToCombo(FormLayout form, Position item) {
        _reportsCombo = new ComboBox<>("Reports To");
        List<Position> allPositions = getExistingItems();
        Position nilPosition = new Position("nil");
        nilPosition.setID("PO-nil");
        initCombo(_reportsCombo, allPositions, _reportsTo, nilPosition);
        form.add(_reportsCombo, 1);
    }

    @Override
    void addGroupCombo(FormLayout form, Position item) {
        _belongsCombo = new ComboBox<>("Belongs To");
        OrgGroup nilGroup = new OrgGroup();
        nilGroup.setLabel("nil");
        nilGroup.setID("OG-nil");
        _allGroups.add(0, nilGroup);
        _belongsCombo.setItems(_allGroups);
        _belongsCombo.setItemLabelGenerator(OrgGroup::getName);
        if (isEditing() && _belongsTo != null) {
            _belongsCombo.setValue(_belongsTo);
        }
        else {
            _belongsCombo.setValue(nilGroup);
        }

        form.add(_belongsCombo, 1);
    }


    @Override
    public String getMemberHeight() { return "290px"; }

    @Override
    public String getSelectMembersHeight() { return "257px"; }

    @Override
    public Position compose() {
        Position position = isEditing() ? getItem() : new Position();
        position.setLabel(getNameValue());
        position.setDescription(getDescriptionValue());
        position.setNotes(getNotesValue());

        Position reportsTo = _reportsCombo.getValue();
        if (reportsTo.getName().equals("nil")) {
            reportsTo = null;
        }
        position.setReportsTo(reportsTo);

        OrgGroup belongsTo = _belongsCombo.getValue();
        if (belongsTo.getName().equals("nil")) {
            belongsTo = null;
        }
        position.setOrgGroup(belongsTo);

        return position;
    }


    @Override
    public boolean validate() {
        return super.validate() &
                validateCombo(_reportsCombo, "position", "report");
    }

}
