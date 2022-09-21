package org.yawlfoundation.yawl.ui.dialog.orgdata;

import com.vaadin.flow.component.formlayout.FormLayout;
import org.yawlfoundation.yawl.resourcing.resource.Capability;
import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.List;

/**
 * @author Michael Adams
 * @date 9/9/2022
 */
public class CapabilityDialog extends AbstractOrgDataDialog<Capability> {


    public CapabilityDialog(List<Capability> items, Capability item,
                            List<Participant> allParticipants,
                            List<Participant> members) {
        super(items, item, allParticipants, members, "Capability");
        build();
        setWidth("700px");
    }


    @Override
    void addBelongsToCombo(FormLayout form, Capability item) { }


    @Override
    void addGroupCombo(FormLayout form, Capability item) { }


    @Override
    String checkCyclicReferences(List<Capability> items, Capability item) {
        return null;          // no belongs to -> no check required
    }


    @Override
    public Capability compose() {
        Capability capability = isEditing() ? getItem() : new Capability();
        capability.setCapability(getNameValue());
        capability.setDescription(getDescriptionValue());
        capability.setNotes(getNotesValue());
        return capability;
    }
}
