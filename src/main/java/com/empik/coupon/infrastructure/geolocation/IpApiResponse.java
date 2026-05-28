package com.empik.coupon.infrastructure.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record IpApiResponse(
    String status,
    @JsonProperty("countryCode") String countryCode,
    String message
) {
    boolean isSuccess() {
        return "success".equals(status);
    }
}
