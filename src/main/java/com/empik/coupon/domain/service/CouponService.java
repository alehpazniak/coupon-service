package com.empik.coupon.domain.service;

import com.empik.coupon.domain.exception.*;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import com.empik.coupon.domain.port.CouponUsageRepositoryPort;
import com.empik.coupon.domain.port.GeolocationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepositoryPort couponRepository;
    private final CouponUsageRepositoryPort couponUsageRepository;
    private final GeolocationPort geolocationPort;

    public CouponService(
            CouponRepositoryPort couponRepository,
            CouponUsageRepositoryPort couponUsageRepository,
            GeolocationPort geolocationPort) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.geolocationPort = geolocationPort;
    }

    @Transactional
    public Coupon createCoupon(String code, int maxUsages, String country) {
        String normalizedCode = code.toUpperCase().trim();

        if (couponRepository.existsByCode(normalizedCode)) {
            throw new CouponAlreadyExistsException(normalizedCode);
        }

        Coupon coupon = Coupon.create(normalizedCode, maxUsages, country);
        Coupon saved = couponRepository.save(coupon);

        log.info("Coupon created: code={}, maxUsages={}, country={}",
            saved.getCode(), saved.getMaxUsages(), saved.getCountry());

        return saved;
    }

    /**
     * Registers a coupon usage for the given user.
     *
     * <p>A pessimistic write lock is acquired on the coupon row for the duration of the transaction,
     * ensuring that concurrent requests for the same coupon are serialized. This prevents
     * over-counting (exceeding maxUsages) under high concurrency without requiring application-level
     * retry logic.</p>
     *
     * @param code      coupon code (case-insensitive)
     * @param userId    identifier of the user attempting to use the coupon
     * @param clientIp  the originating IP address, used for country resolution
     * @return the updated coupon after a successful use
     */
    @Transactional
    public Coupon useCoupon(String code, String userId, String clientIp) {
        String normalizedCode = code.toUpperCase().trim();

        Coupon coupon = couponRepository.findByCodeWithLock(normalizedCode)
            .orElseThrow(() -> new CouponNotFoundException(normalizedCode));

        if (coupon.isExhausted()) {
            throw new CouponExhaustedException(normalizedCode);
        }

        String userCountry = geolocationPort.getCountryCode(clientIp);
        if (!coupon.isAvailableForCountry(userCountry)) {
            log.warn("Coupon use rejected — country mismatch: coupon={}, allowed={}, requestCountry={}",
                normalizedCode, coupon.getCountry(), userCountry);
            throw new CouponCountryMismatchException(normalizedCode, coupon.getCountry(), userCountry);
        }

        if (couponUsageRepository.existsByCouponAndUserId(coupon, userId)) {
            throw new CouponAlreadyUsedException(normalizedCode, userId);
        }

        coupon.use();
        couponRepository.save(coupon);
        couponUsageRepository.save(CouponUsage.of(coupon, userId));

        log.info("Coupon used: code={}, userId={}, remaining={}",
            normalizedCode, userId, coupon.remainingUsages());

        return coupon;
    }
}
