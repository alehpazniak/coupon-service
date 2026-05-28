package com.empik.coupon.domain.exception;

public class CouponExhaustedException extends RuntimeException {
    public CouponExhaustedException(String code) {
        super("Coupon '%s' has reached its maximum usage limit".formatted(code));
    }
}
