package org.yawlfoundation.yawl.ui.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 22/8/2022
 */
abstract class AbstractClient {

    private static final Set<ClientEventListener> listeners = new HashSet<>();

    protected String _handle;

    protected AbstractClient() { }


    abstract void connect() throws IOException;

    abstract void disconnect() throws IOException;

    abstract boolean connected() throws IOException;


    protected String getHandle() throws IOException {
        connect();
        return _handle;
    }


    public void addEventListener(ClientEventListener listener) {
        listeners.add(listener);
    }


    public void removeEventListener(ClientEventListener listener) {
        listeners.remove(listener);
    }


    protected void announceEvent(ClientEvent event) {
        listeners.forEach(l -> l.onClientEvent(event));
    }


    protected void announceEvent(ClientEvent.Action action, Object object) {
        announceEvent(new ClientEvent(action, object));
    }
}
