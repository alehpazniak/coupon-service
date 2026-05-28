package com.empik.coupon.domain.port;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;

public interface CouponUsageRepositoryPort {
    CouponUsage save(CouponUsage usage);
    boolean existsByCouponAndUserId(Coupon coupon, String userId);
}
