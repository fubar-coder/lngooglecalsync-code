package LotusNotesGoogleCalendarBridge.ProxyModule;

public class ProxyConfigBean {

    public ProxyConfigBean() {
        proxyHost = "";
        proxyPort = "";
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void activateNow() {
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", proxyHost);
        System.getProperties().put("proxyPort", proxyPort);
    }

    public void deactivateNow() {
        System.getProperties().put("proxySet", "false");
        System.getProperties().put("proxyHost", "");
        System.getProperties().put("proxyPort", "");
    }
    String proxyHost, proxyPort;
    boolean enabled;
}