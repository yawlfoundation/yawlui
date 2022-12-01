package org.yawlfoundation.yawl.ui.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.yawlfoundation.yawl.util.XNode;
import org.yawlfoundation.yawl.util.XNodeParser;

import java.io.IOException;
import java.util.*;

/**
 * @author Michael Adams
 * @date 22/8/2022
 */
public abstract class AbstractClient {

    private static final ImmutablePair<String, String> DEFAULTS =
            new ImmutablePair<>("admin", "YAWL");
    private static final ImmutablePair<String, String> SERVICE =
            new ImmutablePair<>("yawlUI", "yYUI");

    private static final Set<ClientEventListener> listeners = new HashSet<>();

    protected String _handle;

    protected AbstractClient() { }


    abstract void connect() throws IOException;

    abstract void disconnect() throws IOException;

    abstract boolean connected() throws IOException;

    public abstract Map<String, String> getBuildProperties() throws IOException;


    protected ImmutablePair<String, String> getPair() { return SERVICE; }

    protected ImmutablePair<String, String> getDefaults() { return DEFAULTS; }



    protected String getHandle() throws IOException {
        connect();
        return _handle;
    }


    protected String buildURI(String host, String port, String path) {
        return String.format("http://%s:%s/%s", host, port, path);
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


    protected Map<String, String> buildPropertiesToMap(String xml) {
        XNode node = new XNodeParser().parse(xml);
        if (node != null) {
            Map<String, String> map = new HashMap<>();
            node.getChildren().forEach(child -> map.put(child.getName(), child.getText()));
            return map;
        }
        return Collections.emptyMap();
    }

}
