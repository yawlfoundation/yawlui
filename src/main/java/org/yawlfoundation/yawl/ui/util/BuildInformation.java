package org.yawlfoundation.yawl.ui.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * @author Michael Adams
 * @date 28/11/2022
 */
public class BuildInformation {

    private final Properties BUILD_PROPS = new Properties();

    public BuildInformation() {
        load();
    }


    public String get(String key) {
        return BUILD_PROPS.getProperty(key);
    }

    public BuildProperties getUIProperties() {
        return getProperties("ui");
    }

    public BuildProperties getMailServiceProperties() {
        return getProperties("mail");
    }


    public BuildProperties getInvokerServiceProperties() {
        return getProperties("invoker");
    }


    public BuildProperties getDocStoreProperties() {
        return getProperties("docstore");
    }


    public BuildProperties getProperties(String prefix) {
        String version = get(prefix + ".service.version");
        String number = get(prefix + ".service.build");
        String  date = get(prefix + ".service.build.date");
        return new BuildProperties(version, number, date);
    }


    private void load() {
        InputStream is = BuildInformation.class.getResourceAsStream(
                "/build.properties");
        if (is != null) {
            try {
                BUILD_PROPS.load(is);
            }
            catch (IOException e) {
                // no props to load
            }
        }
    }


    public static class BuildProperties {
        public String version;
        public String number;
        public String date;

        protected BuildProperties(String v, String n, String d) {
            version = v;
            number = n;
            date = d;
        }

        public Map<String, String> asMap() {
            return Map.of("BuildDate", date, "Version", version,
                                "BuildNumber", number);
        }

    }

}
