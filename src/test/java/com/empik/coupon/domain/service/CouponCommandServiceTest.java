package com.empik.coupon.domain.service;

import com.empik.coupon.domain.exception.*;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import com.empik.coupon.domain.port.CouponUsageRepositoryPort;
import com.empik.coupon.domain.port.GeolocationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponCommandServiceTest {

    @Mock CouponRepositoryPort couponRepository;
    @Mock CouponUsageRepositoryPort couponUsageRepository;
    @Mock GeolocationPort geolocationPort;

    CouponCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new CouponCommandService(couponRepository, couponUsageRepository, geolocationPort);
    }

    // =========================================================================
    // createCoupon
    // =========================================================================

    @Nested
    class CreateCoupon {

        @Test
        void savesNewCouponAndReturnsIt() {
            given(couponRepository.existsByCode("WIOSNA")).willReturn(false);
            given(couponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Coupon result = commandService.createCoupon("wiosna", 10, "PL");

            assertThat(result.getCode()).isEqualTo("WIOSNA");
            assertThat(result.getMaxUsages()).isEqualTo(10);
            assertThat(result.getCountry()).isEqualTo("PL");
            assertThat(result.getCurrentUsages()).isZero();
        }

        @Test
        void normalizesCodeToUpperCase_beforeCheckingForDuplicates() {
            given(couponRepository.existsByCode("WIOSNA")).willReturn(false);
            given(couponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            commandService.createCoupon("wiosna", 10, "PL");

            then(couponRepository).should().existsByCode("WIOSNA");
            then(couponRepository).should(never()).existsByCode("wiosna");
        }

        @Test
        void throwsCouponAlreadyExistsException_whenCodeTaken() {
            given(couponRepository.existsByCode("WIOSNA")).willReturn(true);

            assertThatThrownBy(() -> commandService.createCoupon("WIOSNA", 10, "PL"))
                .isInstanceOf(CouponAlreadyExistsException.class)
                .hasMessageContaining("WIOSNA");

            then(couponRepository).should(never()).save(any());
        }

        @Test
        void throwsCouponAlreadyExistsException_forCodeDifferingOnlyInCase() {
            given(couponRepository.existsByCode("WIOSNA")).willReturn(true);

            assertThatThrownBy(() -> commandService.createCoupon("wiosna", 5, "PL"))
                .isInstanceOf(CouponAlreadyExistsException.class);
        }

        @Test
        void savedCouponHasNormalizedCodeAndCountry() {
            given(couponRepository.existsByCode(anyString())).willReturn(false);
            given(couponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var captor = ArgumentCaptor.forClass(Coupon.class);
            commandService.createCoupon("  summer  ", 50, "de");

            then(couponRepository).should().save(captor.capture());
            Coupon saved = captor.getValue();
            assertThat(saved.getCode()).isEqualTo("SUMMER");
            assertThat(saved.getCountry()).isEqualTo("DE");
            assertThat(saved.getMaxUsages()).isEqualTo(50);
        }
    }

    // =========================================================================
    // useCoupon
    // =========================================================================

    @Nested
    class UseCoupon {

        private static final String CLIENT_IP = "1.2.3.4";
        private static final String USER_ID   = "user-42";

        @Test
        void successfullyUsesCoupon_andReturnsUpdatedCoupon() {
            Coupon coupon = Coupon.create("SUMMER", 5, "PL");
            given(couponRepository.findByCodeWithLock("SUMMER")).willReturn(Optional.of(coupon));
            given(geolocationPort.getCountryCode(CLIENT_IP)).willReturn("PL");
            given(couponUsageRepository.existsByCouponAndUserId(coupon, USER_ID)).willReturn(false);
            given(couponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Coupon result = commandService.useCoupon("SUMMER", USER_ID, CLIENT_IP);

            assertThat(result.getCurrentUsages()).isEqualTo(1);
            assertThat(result.remainingUsages()).isEqualTo(4);
            then(couponUsageRepository).should().save(any());
        }

        @Test
        void normalizesCodeToUpperCase_beforeLookup() {
            Coupon coupon = Coupon.create("SUMMER", 5, "PL");
            given(couponRepository.findByCodeWithLock("SUMMER")).willReturn(Optional.of(coupon));
            given(geolocationPort.getCountryCode(CLIENT_IP)).willReturn("PL");
            given(couponUsageRepository.existsByCouponAndUserId(any(), any())).willReturn(false);
            given(couponRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            commandService.useCoupon("summer", USER_ID, CLIENT_IP);

            then(couponRepository).should().findByCodeWithLock("SUMMER");
            then(couponRepository).should(never()).findByCodeWithLock("summer");
        }

        @Test
        void throwsCouponNotFoundException_whenCodeDoesNotExist() {
            given(couponRepository.findByCodeWithLock("GHOST")).willReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.useCoupon("GHOST", USER_ID, CLIENT_IP))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("GHOST");

            then(geolocationPort).shouldHaveNoInteractions();
            then(couponUsageRepository).shouldHaveNoInteractions();
        }

        @Test
        void throwsCouponExhaustedException_whenNoUsagesRemain() {
            Coupon exhausted = Coupon.create("FULL", 1, "PL");
            exhausted.use();
            given(couponRepository.findByCodeWithLock("FULL")).willReturn(Optional.of(exhausted));

            assertThatThrownBy(() -> commandService.useCoupon("FULL", USER_ID, CLIENT_IP))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessageContaining("FULL");

            then(geolocationPort).shouldHaveNoInteractions();
            then(couponUsageRepository).shouldHaveNoInteractions();
        }

        @Test
        void checksExhaustion_beforeCallingGeolocation_toAvoidUnnecessaryExternalCall() {
            Coupon exhausted = Coupon.create("X", 1, "PL");
            exhausted.use();
            given(couponRepository.findByCodeWithLock("X")).willReturn(Optional.of(exhausted));

            assertThatThrownBy(() -> commandService.useCoupon("X", USER_ID, CLIENT_IP))
                .isInstanceOf(CouponExhaustedException.class);

            then(geolocationPort).shouldHaveNoInteractions();
        }

        @Test
        void throwsCouponCountryMismatchException_whenUserIsFromWrongCountry() {
            Coupon coupon = Coupon.create("PL-ONLY", 10, "PL");
            given(couponRepository.findByCodeWithLock("PL-ONLY")).willReturn(Optional.of(coupon));
            given(geolocationPort.getCountryCode(CLIENT_IP)).willReturn("DE");

            assertThatThrownBy(() -> commandService.useCoupon("PL-ONLY", USER_ID, CLIENT_IP))
                .isInstanceOf(CouponCountryMismatchException.class)
                .hasMessageContaining("PL-ONLY");

            then(couponUsageRepository).shouldHaveNoInteractions();
            then(couponRepository).should(never()).save(any());
        }

        @Test
        void throwsCouponAlreadyUsedException_whenUserAlreadyUsedThisCoupon() {
            Coupon coupon = Coupon.create("PROMO", 10, "PL");
            given(couponRepository.findByCodeWithLock("PROMO")).willReturn(Optional.of(coupon));
            given(geolocationPort.getCountryCode(CLIENT_IP)).willReturn("PL");
            given(couponUsageRepository.existsByCouponAndUserId(coupon, USER_ID)).willReturn(true);

            assertThatThrownBy(() -> commandService.useCoupon("PROMO", USER_ID, CLIENT_IP))
                .isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining(USER_ID)
                .hasMessageContaining("PROMO");

            then(couponRepository).should(never()).save(any());
        }

        @Test
        void doesNotPersistAnything_whenGeolocationFails() {
            Coupon coupon = Coupon.create("X", 5, "PL");
            given(couponRepository.findByCodeWithLock("X")).willReturn(Optional.of(coupon));
            given(geolocationPort.getCountryCode(CLIENT_IP))
                .willThrow(new GeolocationServiceException("Service down"));

            assertThatThrownBy(() -> commandService.useCoupon("X", USER_ID, CLIENT_IP))
                .isInstanceOf(GeolocationServiceException.class);

            then(couponUsageRepository).shouldHaveNoInteractions();
            then(couponRepository).should(never()).save(any());
        }
    }
}
