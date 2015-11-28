package org.sriki.githistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

public final class LoaderProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderProperties.class);
    private static final LoaderProperties ME = new LoaderProperties();
    private static final int INVALID_VALUE = Integer.MIN_VALUE;
    public static final String PROPERTIES_FILE ="loader.properties";
    public static final String FILE_NAME_PROPERTY = "loader.propertiesFile";
    private Properties properties = new Properties();

    private void loadProperties(File file) {
        if (!file.exists()) {
            return;
        }
        LOGGER.info("Loading properties file: {}",file.getAbsolutePath());
        try {
            properties.load(FileUtils.openInputStream(file));
            setSystemProperties();
        } catch (Exception e) {
            LOGGER.error("Failed to load propertis file: " + file, e);
        }
    }

    private void setSystemProperties() {
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            String systemValue = System.getProperty(name);
            if (StringUtils.isBlank(systemValue)) {
                setProperty(name, StringUtils.trim(properties.getProperty(name)));
            } else {
                properties.put(name, systemValue);
            }
        }
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    public int getIntProperty(String name) {
        return getIntProperty(name, INVALID_VALUE);
    }

    public long getLongProperty(String name) {
        return getLongProperty(name, INVALID_VALUE);
    }

    public boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public int getIntProperty(String name, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(name, "" + defaultValue));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLongProperty(String name, long defaultValue) {
        try {
            return Long.parseLong(getProperty(name, "" + defaultValue));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getProperty(name, "" + defaultValue));
        } catch (Exception e) {
            return false;
        }
    }

    private File getPropertiesFile() {
        return new File(System.getProperty(FILE_NAME_PROPERTY, PROPERTIES_FILE));
    }

    public static final LoaderProperties getInstance() {
        return ME;
    }

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
        System.setProperty(name, value);
    }

    public void setPropertyIfAbsent(String name, String value) {
        if (properties.getProperty(name) != null || System.getProperty(name) != null) {
            return;
        }
        setProperty(name, value);
    }


    public void clear() {
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            System.clearProperty((String) propertyNames.nextElement());
        }
        properties.clear();
    }

    public void init() {
        loadProperties(getPropertiesFile());
    }
}
