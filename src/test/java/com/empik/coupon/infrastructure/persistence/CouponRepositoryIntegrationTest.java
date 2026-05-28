package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.PostgresIntegrationTest;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.model.CouponUsage;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import com.empik.coupon.domain.port.CouponUsageRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Transactional
class CouponRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    CouponRepositoryPort couponRepository;

    @Autowired
    CouponUsageRepositoryPort couponUsageRepository;

    // -------------------------------------------------------------------------
    // CouponRepository
    // -------------------------------------------------------------------------

    @Test
    void savesAndFindsBack_byCode() {
        Coupon coupon = couponRepository.save(Coupon.create("TESTCODE", 5, "PL"));

        assertThat(couponRepository.findByCode("TESTCODE"))
            .isPresent()
            .get()
            .extracting(Coupon::getCode, Coupon::getMaxUsages, Coupon::getCountry)
            .containsExactly("TESTCODE", 5, "PL");
    }

    @Test
    void findByCode_returnsEmpty_whenCodeDoesNotExist() {
        assertThat(couponRepository.findByCode("NONEXISTENT")).isEmpty();
    }

    @Test
    void existsByCode_returnsTrue_afterSave() {
        couponRepository.save(Coupon.create("EXISTS", 1, "PL"));

        assertThat(couponRepository.existsByCode("EXISTS")).isTrue();
    }

    @Test
    void existsByCode_returnsFalse_whenNeverSaved() {
        assertThat(couponRepository.existsByCode("GHOST")).isFalse();
    }

    @Test
    void findByCodeWithLock_returnsCorrectCoupon() {
        couponRepository.save(Coupon.create("LOCKED", 3, "DE"));

        assertThat(couponRepository.findByCodeWithLock("LOCKED"))
            .isPresent()
            .get()
            .extracting(Coupon::getCode)
            .isEqualTo("LOCKED");
    }

    @Test
    void updatesCurrentUsages_afterUseAndSave() {
        Coupon coupon = couponRepository.save(Coupon.create("USE-ME", 5, "PL"));
        coupon.use();
        couponRepository.save(coupon);

        Coupon reloaded = couponRepository.findByCode("USE-ME").orElseThrow();
        assertThat(reloaded.getCurrentUsages()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // CouponUsageRepository
    // -------------------------------------------------------------------------

    @Test
    void savesUsageAndDetectsItForSameUserAndCoupon() {
        Coupon coupon = couponRepository.save(Coupon.create("PROMO", 10, "PL"));

        couponUsageRepository.save(CouponUsage.of(coupon, "user-1"));

        assertThat(couponUsageRepository.existsByCouponAndUserId(coupon, "user-1")).isTrue();
    }

    @Test
    void existsByCouponAndUserId_returnsFalse_forDifferentUser() {
        Coupon coupon = couponRepository.save(Coupon.create("PROMO2", 10, "PL"));
        couponUsageRepository.save(CouponUsage.of(coupon, "user-1"));

        assertThat(couponUsageRepository.existsByCouponAndUserId(coupon, "user-2")).isFalse();
    }

    @Test
    void existsByCouponAndUserId_returnsFalse_forDifferentCoupon() {
        Coupon couponA = couponRepository.save(Coupon.create("PROMO-A", 10, "PL"));
        Coupon couponB = couponRepository.save(Coupon.create("PROMO-B", 10, "PL"));
        couponUsageRepository.save(CouponUsage.of(couponA, "user-1"));

        assertThat(couponUsageRepository.existsByCouponAndUserId(couponB, "user-1")).isFalse();
    }
}
