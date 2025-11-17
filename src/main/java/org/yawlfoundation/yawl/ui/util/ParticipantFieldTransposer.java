package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.resourcing.resource.Participant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Adams
 * @date 6/11/2025
 */
public class ParticipantFieldTransposer {

    private final List<Participant> _participants;
    private Map<String, String> _kvLookup = new HashMap<>();
    private Field _from;
    private Field _to;

    
    public enum Field {
        Key ("Anonymous ID"),
        UserID ("User ID"),
        FullName ("Firstname Lastname"),
        ReversedName ("Lastname, Firstname");

        private String label;

        Field(String s) { label = s; }

        public String getLabel() { return label; }
    }


    public ParticipantFieldTransposer(List<Participant> pList) {
        _participants = pList;
    }


    public void setFields(Field from, Field to) {
        _from = from;
        _to = to;
    }

    public String transpose(String fromValue) {
        return transpose(_from, _to, fromValue);
    }

    public String transpose(Field from, Field to, String fromValue) {
        if (_kvLookup.containsKey(fromValue)) {
            return _kvLookup.get(fromValue);
        }
        Participant p = getParticipant(from, fromValue);
       if (p != null) {
           String value = null;
           switch (to) {
               case Key: value = p.getID(); break;
               case UserID: value = p.getUserID(); break;
               case FullName: value = p.getFullName(); break;
               case ReversedName: value = p.getLastName() + ", " + p.getFirstName(); break;
           }
           if (value != null) {
               _kvLookup.put(fromValue, value);
               return value;
           }
       }
       return null;
    }


    public Map<String, String> getTransposeMap() { return _kvLookup; }


    private Participant getParticipant(Field field, String key) {
        for (Participant p : _participants) {
            switch (field) {
                case Key: if (p.getID().equals(key)) return p; break;
                case UserID: if (p.getUserID().equals(key)) return p; break;
            }
        }
        return null;
    }

}
