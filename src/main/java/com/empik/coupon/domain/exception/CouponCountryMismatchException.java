package com.empik.coupon.domain.exception;

public class CouponCountryMismatchException extends RuntimeException {
    public CouponCountryMismatchException(String code, String allowedCountry, String requestCountry) {
        super("Coupon '%s' is not available in country '%s' (coupon is restricted to '%s')"
            .formatted(code, requestCountry, allowedCountry));
    }
}
