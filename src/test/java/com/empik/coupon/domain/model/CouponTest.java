package com.empik.coupon.domain.model;

import com.empik.coupon.domain.exception.CouponExhaustedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CouponTest {

    // -------------------------------------------------------------------------
    // factory
    // -------------------------------------------------------------------------

    @Test
    void create_normalizesCodeToUpperCase() {
        Coupon coupon = Coupon.create("wiosna", 10, "PL");
        assertThat(coupon.getCode()).isEqualTo("WIOSNA");
    }

    @Test
    void create_normalizesCountryToUpperCase() {
        Coupon coupon = Coupon.create("CODE", 5, "pl");
        assertThat(coupon.getCountry()).isEqualTo("PL");
    }

    @Test
    void create_trimsPaddingFromCodeAndCountry() {
        Coupon coupon = Coupon.create("  SUMMER  ", 3, " de ");
        assertThat(coupon.getCode()).isEqualTo("SUMMER");
        assertThat(coupon.getCountry()).isEqualTo("DE");
    }

    @Test
    void create_setsInitialUsagesToZero() {
        Coupon coupon = Coupon.create("X", 5, "PL");
        assertThat(coupon.getCurrentUsages()).isZero();
    }

    @Test
    void create_setsCreatedAtToNow() {
        Coupon coupon = Coupon.create("X", 1, "PL");
        assertThat(coupon.getCreatedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // use()
    // -------------------------------------------------------------------------

    @Test
    void use_incrementsCurrentUsages() {
        Coupon coupon = Coupon.create("X", 3, "PL");

        coupon.use();

        assertThat(coupon.getCurrentUsages()).isEqualTo(1);
    }

    @Test
    void use_allowsExactlyMaxUsagesTimes() {
        Coupon coupon = Coupon.create("X", 3, "PL");

        coupon.use();
        coupon.use();
        coupon.use();

        assertThat(coupon.getCurrentUsages()).isEqualTo(3);
        assertThat(coupon.isExhausted()).isTrue();
    }

    @Test
    void use_throwsCouponExhaustedExceptionWhenLimitReached() {
        Coupon coupon = Coupon.create("X", 1, "PL");
        coupon.use();

        assertThatThrownBy(coupon::use)
            .isInstanceOf(CouponExhaustedException.class)
            .hasMessageContaining("X");
    }

    @Test
    void use_doesNotModifyUsagesAfterThrowingException() {
        Coupon coupon = Coupon.create("X", 1, "PL");
        coupon.use();

        assertThatThrownBy(coupon::use).isInstanceOf(CouponExhaustedException.class);

        assertThat(coupon.getCurrentUsages()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // isExhausted()
    // -------------------------------------------------------------------------

    @Test
    void isExhausted_returnsFalse_whenNoUsagesYet() {
        Coupon coupon = Coupon.create("X", 5, "PL");
        assertThat(coupon.isExhausted()).isFalse();
    }

    @Test
    void isExhausted_returnsTrue_whenCurrentEqualsMax() {
        Coupon coupon = Coupon.create("X", 2, "PL");
        coupon.use();
        coupon.use();

        assertThat(coupon.isExhausted()).isTrue();
    }

    // -------------------------------------------------------------------------
    // isAvailableForCountry()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"PL, PL", "PL, pl", "PL, Pl", "de, DE", "de, de"})
    void isAvailableForCountry_isCaseInsensitive(String couponCountry, String requestCountry) {
        Coupon coupon = Coupon.create("X", 10, couponCountry);
        assertThat(coupon.isAvailableForCountry(requestCountry)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DE", "FR", "US", "en"})
    void isAvailableForCountry_returnsFalse_forDifferentCountry(String requestCountry) {
        Coupon coupon = Coupon.create("X", 10, "PL");
        assertThat(coupon.isAvailableForCountry(requestCountry)).isFalse();
    }

    // -------------------------------------------------------------------------
    // remainingUsages()
    // -------------------------------------------------------------------------

    @Test
    void remainingUsages_equalsMaxWhenFresh() {
        Coupon coupon = Coupon.create("X", 5, "PL");
        assertThat(coupon.remainingUsages()).isEqualTo(5);
    }

    @Test
    void remainingUsages_decreasesWithEachUse() {
        Coupon coupon = Coupon.create("X", 3, "PL");
        coupon.use();

        assertThat(coupon.remainingUsages()).isEqualTo(2);
    }

    @Test
    void remainingUsages_isZeroWhenExhausted() {
        Coupon coupon = Coupon.create("X", 2, "PL");
        coupon.use();
        coupon.use();

        assertThat(coupon.remainingUsages()).isZero();
    }
}
