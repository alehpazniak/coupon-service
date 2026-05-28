package com.empik.coupon.domain.exception;

public class CouponAlreadyExistsException extends RuntimeException {
    public CouponAlreadyExistsException(String code) {
        super("Coupon with code '%s' already exists".formatted(code));
    }
}
