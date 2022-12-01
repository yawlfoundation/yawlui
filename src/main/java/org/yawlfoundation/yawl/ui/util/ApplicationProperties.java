package org.yawlfoundation.yawl.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Michael Adams
 * @date 28/11/2022
 */
public class ApplicationProperties {

    private static final Properties props = new Properties();
    
    private ApplicationProperties() { }


    public static String get(String key) {
        if (props.isEmpty()) {
            load();
        }
        return props.getProperty(key);
    }


    public static String getEngineHost() {
        return getHost(get("engine.host"));
    }


    public static String getEnginePort() {
        return getPort(get("engine.port"));
    }


    public static String getResourceServiceHost() {
        return getHost(get("resource.service.host"));
    }


    public static String getResourceServicePort() {
        return getPort(get("resource.service.port"));
    }


    public static String getWorkletServiceHost() {
        return getHost(get("worklet.service.host"));
    }


    public static String getWorkletServicePort() {
        return getPort(get("worklet.service.port"));
    }


    public static String getDocStoreHost() {
        return getHost(get("document.store.host"));
    }


    public static String getDocStorePort() {
        return getPort(get("document.store.port"));
    }


    private static String getBaseHost() {
        String host = get("base.host");
        return host.isEmpty() ? "localhost" : host;
    }


    private static String getBasePort() {
        String port = get("base.port");
        return port.isEmpty() ? "8080" : port;
    }


    private static String getHost(String host) {
        return host.isEmpty() ? getBaseHost() : host;
    }


    private static String getPort(String port) {
        return port.isEmpty() ? getBasePort() : port;
    }


    private static void load() {
        InputStream is = ApplicationProperties.class.getResourceAsStream(
                "/application.properties");
        if (is != null) {
            try {
                props.load(is);
            }
            catch (IOException e) {
                // no props to load
            }
        }
    }

}
