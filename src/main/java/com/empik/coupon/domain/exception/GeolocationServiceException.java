package com.empik.coupon.domain.exception;

public class GeolocationServiceException extends RuntimeException {
    public GeolocationServiceException(String message) {
        super(message);
    }

    public GeolocationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
