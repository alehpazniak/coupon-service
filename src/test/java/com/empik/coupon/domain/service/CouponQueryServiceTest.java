package com.empik.coupon.domain.service;

import com.empik.coupon.domain.exception.CouponNotFoundException;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponQueryServiceTest {

    @Mock
    CouponRepositoryPort couponRepository;

    CouponQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new CouponQueryService(couponRepository);
    }

    @Test
    void returnsCoupon_whenCodeExists() {
        Coupon coupon = Coupon.create("PROMO", 5, "PL");
        given(couponRepository.findByCode("PROMO")).willReturn(Optional.of(coupon));

        assertThat(queryService.getByCode("PROMO").getCode()).isEqualTo("PROMO");
    }

    @Test
    void throwsCouponNotFoundException_whenCodeDoesNotExist() {
        given(couponRepository.findByCode("GHOST")).willReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getByCode("GHOST"))
            .isInstanceOf(CouponNotFoundException.class)
            .hasMessageContaining("GHOST");
    }

    @Test
    void normalizesCodeToUpperCase_beforeLookup() {
        Coupon coupon = Coupon.create("PROMO", 5, "PL");
        given(couponRepository.findByCode("PROMO")).willReturn(Optional.of(coupon));

        queryService.getByCode("promo");

        then(couponRepository).should().findByCode("PROMO");
        then(couponRepository).should(never()).findByCode("promo");
    }
}
