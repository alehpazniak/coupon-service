package com.empik.coupon.domain.service;

import com.empik.coupon.domain.exception.CouponNotFoundException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CouponQueryService {

    private final CouponRepositoryPort couponRepository;

    public CouponQueryService(CouponRepositoryPort couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon getByCode(String code) {
        String normalizedCode = code.toUpperCase().trim();
        return couponRepository.findByCode(normalizedCode)
            .orElseThrow(() -> new CouponNotFoundException(normalizedCode));
    }
}
