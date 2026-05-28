package com.empik.coupon.domain.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Coupon with code '%s' not found".formatted(code));
    }
}
