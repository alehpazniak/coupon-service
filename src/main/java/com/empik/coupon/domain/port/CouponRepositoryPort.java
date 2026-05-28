package com.empik.coupon.domain.port;

import com.empik.coupon.domain.model.Coupon;

import java.util.Optional;

public interface CouponRepositoryPort {
    Coupon save(Coupon coupon);
    Optional<Coupon> findByCode(String code);
    Optional<Coupon> findByCodeWithLock(String code);
    boolean existsByCode(String code);
}
