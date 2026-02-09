package org.digitalcampus.oppia.utils;

/**
 * AI backend configuration for the Custom AI Chat feature.
 *
 * Default base URL targets localhost on the Android emulator.
 * - Emulator: http://10.0.2.2:8000
 * - Physical device: use your machine's LAN IP, e.g. http://192.168.1.50:8000
 *
 * Note: "http://localhost" from Android refers to the device/emulator itself,
 * not your development machine.
 */
public final class AiChatConfig {

    private AiChatConfig() {
        throw new IllegalStateException("Utility class");
    }

    public static final String BASE_URL = "http://10.0.2.2:8000";

    /** Optional root path prefix when backend is served behind a path (e.g. "/api"). */
    public static final String ROOT_PATH_PREFIX = "";

    public static String buildUrl(String endpointPath) {
        String base = BASE_URL;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String prefix = ROOT_PATH_PREFIX == null ? "" : ROOT_PATH_PREFIX.trim();
        if ("/".equals(prefix)) prefix = "";
        if (!prefix.isEmpty() && !prefix.startsWith("/")) prefix = "/" + prefix;
        while (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);

        String path = endpointPath == null ? "" : endpointPath.trim();
        if (!path.startsWith("/")) path = "/" + path;

        return base + prefix + path;
    }
}
