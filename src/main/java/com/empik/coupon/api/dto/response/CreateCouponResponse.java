package com.empik.coupon.api.dto.response;

import com.empik.coupon.domain.model.Coupon;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCouponResponse(
    UUID id,
    String code,
    LocalDateTime createdAt,
    int maxUsages,
    int currentUsages,
    String country
) {
    public static CreateCouponResponse from(Coupon coupon) {
        return new CreateCouponResponse(
            coupon.getId(),
            coupon.getCode(),
            coupon.getCreatedAt(),
            coupon.getMaxUsages(),
            coupon.getCurrentUsages(),
            coupon.getCountry()
        );
    }
}
