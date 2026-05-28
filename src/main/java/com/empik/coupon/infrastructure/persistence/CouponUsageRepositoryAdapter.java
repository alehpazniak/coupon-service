package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;
import com.empik.coupon.domain.port.CouponUsageRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
class CouponUsageRepositoryAdapter implements CouponUsageRepositoryPort {

    private final JpaCouponUsageRepository jpaRepository;

    CouponUsageRepositoryAdapter(JpaCouponUsageRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public CouponUsage save(CouponUsage usage) {
        return jpaRepository.save(usage);
    }

    @Override
    public boolean existsByCouponAndUserId(Coupon coupon, String userId) {
        return jpaRepository.existsByCouponAndUserId(coupon, userId);
    }
}
