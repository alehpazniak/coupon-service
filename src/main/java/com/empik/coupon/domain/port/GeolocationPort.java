package com.empik.coupon.domain.port;

/**
 * Port for resolving the country of origin from an IP address.
 * Implementations are responsible for calling external geolocation services.
 */
public interface GeolocationPort {

    /**
     * Returns the ISO 3166-1 alpha-2 country code for the given IP address.
     *
     * @param ipAddress the client IP address
     * @return two-letter country code, e.g. "PL", "DE"
     * @throws com.empik.coupon.domain.exception.GeolocationServiceException
     *         if the country cannot be determined (service unavailable, invalid IP, etc.)
     */
    String getCountryCode(String ipAddress);
}
