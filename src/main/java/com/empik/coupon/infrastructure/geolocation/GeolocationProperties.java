package com.empik.coupon.infrastructure.geolocation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the ip-api.com geolocation service.
 *
 * <p>{@code bypassPrivateIps} is intended for local development: when {@code true},
 * requests from loopback / RFC-1918 addresses skip the external API call and are
 * assigned {@code bypassCountry} instead.
 */
@ConfigurationProperties(prefix = "geolocation")
public record GeolocationProperties(
    String baseUrl,
    boolean bypassPrivateIps,
    String bypassCountry
) {}
