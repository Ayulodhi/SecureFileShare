package com.securefileshare.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration reader.
 * WHAT: Loads and exposes client configuration properties including execution mode (mock vs real)
 *       and base URLs for auth-service, file-service, share-service, and audit-service.
 * WHY: Architecture rule: A single config switch chooses mock or real implementation, and service
 *      endpoints must be configurable via external properties without code changes.
 */
public class AppConfig {

    public static final String DEFAULT_PROPERTIES_FILE = "client.properties";

    public static final String KEY_MODE = "client.mode";
    public static final String KEY_AUTH_URL = "service.auth.url";
    public static final String KEY_FILE_URL = "service.file.url";
    public static final String KEY_SHARE_URL = "service.share.url";
    public static final String KEY_AUDIT_URL = "service.audit.url";

    public static final String MODE_MOCK = "mock";
    public static final String MODE_REAL = "real";

    private static final String DEFAULT_MODE = MODE_MOCK;
    private static final String DEFAULT_AUTH_URL = "http://localhost:8081";
    private static final String DEFAULT_FILE_URL = "http://localhost:8082";
    private static final String DEFAULT_SHARE_URL = "http://localhost:8083";
    private static final String DEFAULT_AUDIT_URL = "http://localhost:8084";

    private static final AppConfig INSTANCE = new AppConfig();

    private String mode;
    private String authServiceUrl;
    private String fileServiceUrl;
    private String shareServiceUrl;
    private String auditServiceUrl;

    /**
     * Constructs AppConfig and automatically loads configuration from classpath.
     */
    public AppConfig() {
        loadDefaults();
        loadFromClasspath(DEFAULT_PROPERTIES_FILE);
    }

    /**
     * Returns the singleton AppConfig instance.
     *
     * @return Global AppConfig instance
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes default fallback configuration values.
     * WHAT: Sets baseline mock mode and standard localhost ports.
     * WHY: Ensures the desktop client boots gracefully even if the properties file is missing.
     */
    private void loadDefaults() {
        this.mode = DEFAULT_MODE;
        this.authServiceUrl = DEFAULT_AUTH_URL;
        this.fileServiceUrl = DEFAULT_FILE_URL;
        this.shareServiceUrl = DEFAULT_SHARE_URL;
        this.auditServiceUrl = DEFAULT_AUDIT_URL;
    }

    /**
     * Loads properties from a classpath resource file.
     * WHAT: Reads input stream from classloader and populates configuration fields.
     * WHY: Supports standard Java application packaging where config sits in src/main/resources.
     *
     * @param resourceName Name of the resource file on classpath
     */
    public void loadFromClasspath(String resourceName) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                loadFromStream(in);
            }
        } catch (IOException ex) {
            System.err.println("Notice: Could not load " + resourceName + ", using default values: " + ex.getMessage());
        }
    }

    /**
     * Loads and applies configuration from an InputStream.
     *
     * @param in InputStream containing Java properties format
     * @throws IOException on read errors
     */
    public void loadFromStream(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        applyProperties(props);
    }

    /**
     * Applies configuration values from a Properties collection.
     * WHAT: Reads specific keys and assigns them to internal state, respecting fallbacks.
     * WHY: Allows direct programmatic configuration in unit tests without file I/O.
     *
     * @param props Properties container
     */
    public void applyProperties(Properties props) {
        if (props == null) return;

        this.mode = props.getProperty(KEY_MODE, this.mode != null ? this.mode : DEFAULT_MODE).trim();
        this.authServiceUrl = props.getProperty(KEY_AUTH_URL, this.authServiceUrl != null ? this.authServiceUrl : DEFAULT_AUTH_URL).trim();
        this.fileServiceUrl = props.getProperty(KEY_FILE_URL, this.fileServiceUrl != null ? this.fileServiceUrl : DEFAULT_FILE_URL).trim();
        this.shareServiceUrl = props.getProperty(KEY_SHARE_URL, this.shareServiceUrl != null ? this.shareServiceUrl : DEFAULT_SHARE_URL).trim();
        this.auditServiceUrl = props.getProperty(KEY_AUDIT_URL, this.auditServiceUrl != null ? this.auditServiceUrl : DEFAULT_AUDIT_URL).trim();
    }

    /**
     * Indicates whether the client is currently running in mock service mode.
     *
     * @return True if mode is "mock" (case-insensitive), false otherwise
     */
    public boolean isMockMode() {
        return MODE_MOCK.equalsIgnoreCase(this.mode);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    public String getFileServiceUrl() {
        return fileServiceUrl;
    }

    public void setFileServiceUrl(String fileServiceUrl) {
        this.fileServiceUrl = fileServiceUrl;
    }

    public String getShareServiceUrl() {
        return shareServiceUrl;
    }

    public void setShareServiceUrl(String shareServiceUrl) {
        this.shareServiceUrl = shareServiceUrl;
    }

    public String getAuditServiceUrl() {
        return auditServiceUrl;
    }

    public void setAuditServiceUrl(String auditServiceUrl) {
        this.auditServiceUrl = auditServiceUrl;
    }
}
