package com.empik.coupon.api.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts the originating client IP address from an HTTP request.
 *
 * <p>Checks well-known proxy headers before falling back to the direct remote address.
 * When {@code X-Forwarded-For} contains a chain of IPs (client, proxy1, proxy2 …),
 * the leftmost (originating) address is returned.
 *
 * <p>In production, ensure that your reverse proxy (nginx, AWS ALB, etc.) is the only
 * entity trusted to set these headers — otherwise they can be spoofed by clients.
 */
public final class IpExtractor {

    private static final String[] PROXY_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    private IpExtractor() {}

    public static String extractClientIp(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
