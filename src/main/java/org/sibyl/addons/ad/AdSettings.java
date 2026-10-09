package org.sibyl.addons.ad;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Immutable and validated LDAPS configuration. Secrets remain in server environment,
 * not in the add-on package, user-facing configuration or logs.
 */
public record AdSettings(URI serverUrl, String baseDn, String bindDn,
                         int pageSize, int connectTimeoutMs, int readTimeoutMs) {
    public AdSettings {
        Objects.requireNonNull(serverUrl, "serverUrl");
        if (!"ldaps".equalsIgnoreCase(serverUrl.getScheme())
                || serverUrl.getHost() == null || serverUrl.getHost().isBlank()
                || serverUrl.getUserInfo() != null || serverUrl.getRawQuery() != null
                || serverUrl.getRawFragment() != null
                || (serverUrl.getPath() != null && !serverUrl.getPath().isBlank())
                || (serverUrl.getPort() != -1 && (serverUrl.getPort() < 1 || serverUrl.getPort() > 65535))) {
            throw new IllegalArgumentException("A DNS-based ldaps:// URL without credentials, path or query is required");
        }
        if (baseDn == null || !baseDn.matches("(?i).*(?:^|,)dc=[^,=]+(?:,dc=[^,=]+)+$") || baseDn.length() > 1024) {
            throw new IllegalArgumentException("baseDn must contain a valid AD domain suffix (DC=...,DC=...)");
        }
        if (bindDn == null || bindDn.isBlank() || bindDn.length() > 1024
                || !bindDn.contains("=")) {
            throw new IllegalArgumentException("A service-account distinguished name is required");
        }
        if (pageSize < 10 || pageSize > 1000) throw new IllegalArgumentException("pageSize must be 10..1000");
        if (connectTimeoutMs < 1000 || connectTimeoutMs > 30000)
            throw new IllegalArgumentException("connectTimeoutMs must be 1000..30000");
        if (readTimeoutMs < 1000 || readTimeoutMs > 60000)
            throw new IllegalArgumentException("readTimeoutMs must be 1000..60000");
    }

    public static AdSettings fromEnvironment() {
        try {
            return new AdSettings(
                new URI(required("SIBYL_AD_URL")),
                required("SIBYL_AD_BASE_DN"),
                required("SIBYL_AD_BIND_DN"),
                numeric("SIBYL_AD_PAGE_SIZE", 250),
                numeric("SIBYL_AD_CONNECT_TIMEOUT_MS", 5000),
                numeric("SIBYL_AD_READ_TIMEOUT_MS", 10000)
            );
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("SIBYL_AD_URL is not a valid URI", ex);
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static int numeric(String key, int fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }
}
