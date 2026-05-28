package com.empik.coupon.api.dto.response;

import com.empik.coupon.domain.model.Coupon;

public record UseCouponResponse(
    String code,
    int remainingUsages,
    String message
) {
    public static UseCouponResponse from(Coupon coupon) {
        return new UseCouponResponse(
            coupon.getCode(),
            coupon.remainingUsages(),
            "Coupon successfully applied"
        );
    }
}
