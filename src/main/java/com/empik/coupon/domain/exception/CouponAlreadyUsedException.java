package com.empik.coupon.domain.exception;

public class CouponAlreadyUsedException extends RuntimeException {
    public CouponAlreadyUsedException(String code, String userId) {
        super("User '%s' has already used coupon '%s'".formatted(userId, code));
    }
}
