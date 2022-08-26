package org.yawlfoundation.yawl.ui.service;

/**
 * @author Michael Adams
 * @date 22/8/2022
 */
public class ClientEvent {

    public enum Action { SpecificationUnload }

    private final Action _action;
    private final Object _object;

    public ClientEvent(Action action, Object object) {
        _action = action;
        _object = object;
    }

    public Action getAction() { return _action; }

    public Object getObject() { return _object; }

}
