package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;
import com.empik.coupon.domain.port.CouponUsageRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCouponUsageRepository extends JpaRepository<CouponUsage, UUID>, CouponUsageRepositoryPort {

    @Override
    boolean existsByCouponAndUserId(Coupon coupon, String userId);
}
