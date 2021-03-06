package preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ConfigurationBean {

    public ConfigurationBean() {
        config = new Properties();
    }

    public void writeConfig() {
        config.setProperty("GoogleUsername", getGoogleUserName());
        config.setProperty("GooglePassword", getGooglePassword());
        config.setProperty("GoogleEnableProxy", new Boolean(getGoogleEnableProxy()).toString());
        config.setProperty("GoogleProxyIP", getGoogleProxyIP());
        config.setProperty("GoogleProxyPort", getGoogleProxyPort());
        config.setProperty("GoogleUseSSL", new Boolean(getGoogleUseSSL()).toString());

        config.setProperty(PROP_NAME_LOTUS_NOTES_SERVER, getLotusNotesServer());
        config.setProperty(PROP_NAME_LOTUS_NOTES_SERVER_IS_LOCAL, new Boolean(getLotusNotesServerIsLocal()).toString());
        config.setProperty(PROP_NAME_LOTUS_NOTES_MAIL_FILE, getLotusNotesMailFile());
        config.setProperty("LotusNotesUsername", getLotusNotesUsername());
        config.setProperty("LotusNotesPassword", getLotusNotesPassword());

        config.setProperty(PROP_SYNC_ON_STARTUP, new Boolean(getSyncOnStartup()).toString());
        config.setProperty(PROP_NAME_DIAGNOSTIC_MODE, new Boolean(getDiagnosticMode()).toString());
        config.setProperty(PROP_SYNC_DESCRIPTION, new Boolean(getSyncDescription()).toString());
        config.setProperty(PROP_SYNC_ALARMS, new Boolean(getSyncAlarms()).toString());

        // Write properties file.
        try {
            config.store(new FileOutputStream(configurationFile), null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void readConfig() {
        try {
            config.load(new FileInputStream(configurationFile));
        } catch (Exception e) {
        }

    }

    public void setLotusNotesServer(String value) {
        config.setProperty(PROP_NAME_LOTUS_NOTES_SERVER, value);
    }

    public void setLotusNotesServerIsLocal(boolean value) {
        setBooleanProperty(PROP_NAME_LOTUS_NOTES_SERVER_IS_LOCAL, value);
    }

    public void setLotusNotesMailFile(String value) {
        config.setProperty(PROP_NAME_LOTUS_NOTES_MAIL_FILE, value);
    }

    public void setGoogleUserName(String googleUserName) {
        config.setProperty("GoogleUsername", googleUserName);
    }

    public void setGooglePassword(String googlePassword) {
        config.setProperty("GooglePassword", googlePassword);
    }

    public void setGoogleProxyIP(String googleProxyIP) {
        config.setProperty("GoogleProxyIP", googleProxyIP);
    }

    public void setGoogleProxyPort(String googleProxyPort) {
        config.setProperty("GoogleProxyPort", googleProxyPort);
    }

    public void setGoogleEnableProxy(boolean googleEnableProxy) {
        setBooleanProperty("GoogleEnableProxy", googleEnableProxy);
    }

    public void setGoogleUseSSL(boolean googleUseSSL) {
        setBooleanProperty("GoogleUseSSL", googleUseSSL);
    }

    public void setSyncOnStartup(boolean value) {
        setBooleanProperty(PROP_SYNC_ON_STARTUP, value);
    }

    public void setDiagnosticMode(boolean value) {
        setBooleanProperty(PROP_NAME_DIAGNOSTIC_MODE, value);
    }

    public void setSyncDescription(boolean value) {
        setBooleanProperty(PROP_SYNC_DESCRIPTION, value);
    }

    public void setSyncAlarms(boolean value) {
        setBooleanProperty(PROP_SYNC_ALARMS, value);
    }

    public void setLotusNotesUsername(String LotusNotesUsername) {
        config.setProperty("LotusNotesUsername", LotusNotesUsername);
    }

    public void setLotusNotesPassword(String LotusNotesPassword) {
        config.setProperty("LotusNotesPassword", LotusNotesPassword);
    }

    protected void setBooleanProperty(String propertyName, boolean propertyValue) {
        String propertyValueStr = "false";
        if (propertyValue) {
            propertyValueStr = "true";
        }

        config.setProperty(propertyName, propertyValueStr);
    }


    public String getLotusNotesServer() {
        String property;

        property = config.getProperty(PROP_NAME_LOTUS_NOTES_SERVER);
        if (property == null) {
            property = "";
        }

        return property;
    }

    public boolean getLotusNotesServerIsLocal() {
        return getBooleanProperty(PROP_NAME_LOTUS_NOTES_SERVER_IS_LOCAL);
    }

    public String getLotusNotesMailFile() {
        String property;

        property = config.getProperty(PROP_NAME_LOTUS_NOTES_MAIL_FILE);
        if (property == null) {
            property = "";
        }

        return property;
    }

    public String getGoogleUserName() {
        String property;

        property = config.getProperty("GoogleUsername");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public String getGooglePassword() {
        String property = null;

        property = config.getProperty("GooglePassword");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public String getGoogleProxyIP() {
        String property = null;

        property = config.getProperty("GoogleProxyIP");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public String getGoogleProxyPort() {
        String property = null;

        property = config.getProperty("GoogleProxyPort");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public boolean getGoogleEnableProxy() {
        return getBooleanProperty("GoogleEnableProxy");
    }

    public boolean getGoogleUseSSL() {
        return getBooleanProperty("GoogleUseSSL");
    }

    public String getLotusNotesUsername() {
        String property = null;

        property = config.getProperty("LotusNotesUsername");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public String getLotusNotesPassword() {
        String property = null;

        property = config.getProperty("LotusNotesPassword");
        if (property == null) {
            property = "";
        }

        return property;
    }

    public boolean getSyncOnStartup() {
        return getBooleanProperty(PROP_SYNC_ON_STARTUP);
    }
    
    public boolean getDiagnosticMode() {
        return getBooleanProperty(PROP_NAME_DIAGNOSTIC_MODE);
    }

    public boolean getSyncDescription() {
        return getBooleanProperty(PROP_SYNC_DESCRIPTION);
    }

    public boolean getSyncAlarms() {
        return getBooleanProperty(PROP_SYNC_ALARMS);
    }

    protected boolean getBooleanProperty(String propertyName) {
        boolean property = true;

        String propertyStr = config.getProperty(propertyName);
        if (propertyStr == null || ! propertyStr.equalsIgnoreCase("true")) {
            property = false;
        }

        return property;
    }

    protected static final String PROP_NAME_LOTUS_NOTES_SERVER = "LotusNotesServer";
    protected static final String PROP_NAME_LOTUS_NOTES_SERVER_IS_LOCAL = "LotusNotesServerIsLocal";
    protected static final String PROP_NAME_LOTUS_NOTES_MAIL_FILE = "LotusNotesMailFile";
    protected static final String PROP_NAME_DIAGNOSTIC_MODE = "DiagnosticMode";
    protected static final String PROP_SYNC_ON_STARTUP = "SyncOnStartup";
    protected static final String PROP_SYNC_DESCRIPTION = "SyncDescription";
    protected static final String PROP_SYNC_ALARMS = "SyncAlarms";

    protected Properties config;
    protected String configurationFile = "lngooglecalsync.properties";
}
