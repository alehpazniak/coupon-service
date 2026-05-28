package com.empik.coupon.infrastructure.geolocation;

import com.empik.coupon.domain.exception.GeolocationServiceException;
import com.empik.coupon.domain.port.GeolocationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;

/**
 * Adapter that resolves country codes using the free ip-api.com service.
 *
 * <p>Private / loopback addresses can be short-circuited for local development
 * via the {@code geolocation.bypass-private-ips} configuration flag.
 *
 * <p>ip-api.com free tier: up to 45 req/min, no API key needed. For production
 * loads, consider a paid tier or a self-hosted alternative (e.g. MaxMind GeoLite2).
 */
@Component
class IpApiGeolocationService implements GeolocationPort {

    private static final Logger log = LoggerFactory.getLogger(IpApiGeolocationService.class);

    private static final Set<String> LOOPBACK_AND_RFC1918_PREFIXES = Set.of(
        "127.", "10.", "192.168.", "::1", "0:0:0:0:0:0:0:1", "0.0.0.0"
    );
    private static final Set<String> RFC1918_172_PREFIXES = Set.of(
        "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
        "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
        "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
    );

    private final RestClient restClient;
    private final GeolocationProperties properties;

    IpApiGeolocationService(RestClient geolocationRestClient, GeolocationProperties properties) {
        this.restClient = geolocationRestClient;
        this.properties = properties;
    }

    @Override
    public String getCountryCode(String ipAddress) {
        if (properties.bypassPrivateIps() && isPrivateIp(ipAddress)) {
            log.debug("Private IP detected ({}), bypassing geolocation — using country: {}",
                ipAddress, properties.bypassCountry());
            return properties.bypassCountry();
        }

        try {
            IpApiResponse response = restClient.get()
                .uri("/json/{ip}?fields=status,countryCode,message", ipAddress)
                .retrieve()
                .body(IpApiResponse.class);

            if (response == null || !response.isSuccess()) {
                String reason = response != null ? response.message() : "null response";
                throw new GeolocationServiceException(
                    "Geolocation lookup failed for IP '%s': %s".formatted(ipAddress, reason));
            }

            log.debug("IP {} resolved to country {}", ipAddress, response.countryCode());
            return response.countryCode();

        } catch (RestClientException e) {
            throw new GeolocationServiceException(
                "Geolocation service unreachable for IP: " + ipAddress, e);
        }
    }

    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        for (String prefix : LOOPBACK_AND_RFC1918_PREFIXES) {
            if (ip.startsWith(prefix)) return true;
        }
        for (String prefix : RFC1918_172_PREFIXES) {
            if (ip.startsWith(prefix)) return true;
        }
        return false;
    }
}
