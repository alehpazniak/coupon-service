package com.empik.coupon.infrastructure.geolocation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the ip-api.com geolocation service.
 *
 * <p>{@code bypassPrivateIps} is intended for local development: when {@code true},
 * requests from loopback / RFC-1918 addresses skip the external API call and are
 * assigned {@code bypassCountry} instead.
 *
 * <p>Timeouts are intentionally short — geolocation is on the hot path of every
 * coupon-use request. A slow external call must fail fast rather than exhaust
 * the server thread pool.
 */
@ConfigurationProperties(prefix = "geolocation")
public record GeolocationProperties(
    String baseUrl,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    boolean bypassPrivateIps,
    String bypassCountry
) {}
