package com.empik.coupon.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extracts the originating client IP address from an HTTP request.
 *
 * <p>Checks well-known reverse-proxy headers before falling back to the direct
 * remote address. When {@code X-Forwarded-For} contains a chain of addresses
 * (client, proxy1, proxy2 …) the leftmost — originating — address is returned.
 *
 * <p><strong>Security note:</strong> in production this component must sit behind
 * a trusted reverse proxy (nginx, AWS ALB, …) that is the only entity allowed to
 * set these headers. Without that boundary a client can spoof an arbitrary IP.
 */
@Component
public class IpExtractor {

    private static final String[] PROXY_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    public String extractClientIp(HttpServletRequest request) {
        for (String header : PROXY_HEADERS) {
            String value = request.getHeader(header);
            if (isValidIpHeaderValue(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIpHeaderValue(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }
}
