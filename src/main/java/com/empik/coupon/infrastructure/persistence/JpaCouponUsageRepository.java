package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface JpaCouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    boolean existsByCouponAndUserId(Coupon coupon, String userId);
}
