package org.yawlfoundation.yawl.ui.util;

import org.yawlfoundation.yawl.ui.dynform.DynFormEnterKeyAction;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Michael Adams
 * @date 28/11/2022
 */
public class ApplicationProperties {

    private static final Properties APP_PROPS = new Properties();
    
    private ApplicationProperties() { }


    public static String get(String key) {
        if (APP_PROPS.isEmpty()) {
            load();
        }
        return APP_PROPS.getProperty(key);
    }


    public static DynFormEnterKeyAction getDynFormEnterKeyAction() {
        return DynFormEnterKeyAction.fromString(get("dyn.form.enter.action"));
    }


    public static boolean suppressSuccessNotifications() {
        return Boolean.parseBoolean(get("suppress.success.notifications"));
    }

    
    public static List<String> getGeoColors() {
        List<String> colours = StringUtil.splitToList(get("geo.colors"), ",");
        for (int i = 0; i < colours.size(); i++) {
            colours.set(i, colours.get(i).trim());
        }
        return colours;
    }


    public static double getGeoFillOpacity() {
        double opacity = StringUtil.strToDouble(get("geo.fill.opacity"), 0);
        if (opacity < 0) { opacity = 0; }
        else if (opacity > 1) { opacity = 1; }
        return opacity;
    }


    public static int getGeoLineWeight() {
        return StringUtil.strToInt(get("geo.line.weight"), 3);
    }


    public static List<Double> getGeoOrigin() {
        List<String> values = StringUtil.splitToList(get("geo.default.origin"), ",");
        if (values.isEmpty()) { return Collections.emptyList(); }
        List<Double> origin = new ArrayList<>();
        for (String value : values) {
            origin.add(StringUtil.strToDouble(value.trim(), 0));
        }
        return origin;
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
                APP_PROPS.load(is);
            }
            catch (IOException e) {
                // no props to load
            }
        }
    }

}
