package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
class CouponRepositoryAdapter implements CouponRepositoryPort {

    private final JpaCouponRepository jpaRepository;

    CouponRepositoryAdapter(JpaCouponRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Coupon save(Coupon coupon) {
        return jpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }

    @Override
    @Transactional
    public Optional<Coupon> findByCodeWithLock(String code) {
        return jpaRepository.findByCodeWithLock(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }
}
