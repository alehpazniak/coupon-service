package com.empik.coupon.api.controller;

import com.empik.coupon.PostgresIntegrationTest;
import com.empik.coupon.domain.exception.*;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.GeolocationPort;
import com.empik.coupon.domain.service.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
class CouponControllerIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CouponService couponService;

    // GeolocationPort is mocked globally in tests to prevent live HTTP calls
    @MockBean GeolocationPort geolocationPort;

    // =========================================================================
    // POST /api/coupons
    // =========================================================================

    @Nested
    class CreateCoupon {

        @Test
        void returns201_withCouponBody_whenRequestIsValid() throws Exception {
            Coupon coupon = Coupon.create("SUMMER", 100, "PL");
            given(couponService.createCoupon("SUMMER", 100, "PL")).willReturn(coupon);

            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"SUMMER","maxUsages":100,"country":"PL"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUMMER"))
                .andExpect(jsonPath("$.maxUsages").value(100))
                .andExpect(jsonPath("$.currentUsages").value(0))
                .andExpect(jsonPath("$.country").value("PL"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        void returns409_whenCouponCodeAlreadyExists() throws Exception {
            given(couponService.createCoupon(any(), anyInt(), any()))
                .willThrow(new CouponAlreadyExistsException("SUMMER"));

            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"SUMMER","maxUsages":5,"country":"PL"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        void returns400_whenCodeIsBlank() throws Exception {
            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"","maxUsages":5,"country":"PL"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='code')]").isNotEmpty());
        }

        @Test
        void returns400_whenMaxUsagesIsZero() throws Exception {
            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"X","maxUsages":0,"country":"PL"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='maxUsages')]").isNotEmpty());
        }

        @Test
        void returns400_whenCountryIsTooLong() throws Exception {
            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"X","maxUsages":5,"country":"POL"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='country')]").isNotEmpty());
        }

        @Test
        void returns400_whenCodeContainsInvalidCharacters() throws Exception {
            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"BAD CODE!","maxUsages":5,"country":"PL"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='code')]").isNotEmpty());
        }

        @Test
        void returns400_whenBodyIsMissing() throws Exception {
            mockMvc.perform(post("/api/coupons")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // POST /api/coupons/{code}/use
    // =========================================================================

    @Nested
    class UseCoupon {

        private static final String VALID_BODY = """
            {"userId":"user-42"}
            """;

        @Test
        void returns200_withRemainingUsages_onSuccess() throws Exception {
            Coupon coupon = Coupon.create("PROMO", 5, "PL");
            coupon.use();
            given(couponService.useCoupon(eq("PROMO"), eq("user-42"), any())).willReturn(coupon);

            mockMvc.perform(post("/api/coupons/PROMO/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PROMO"))
                .andExpect(jsonPath("$.remainingUsages").value(4))
                .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        void returns404_whenCouponNotFound() throws Exception {
            given(couponService.useCoupon(any(), any(), any()))
                .willThrow(new CouponNotFoundException("GHOST"));

            mockMvc.perform(post("/api/coupons/GHOST/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
        }

        @Test
        void returns409_whenCouponIsExhausted() throws Exception {
            given(couponService.useCoupon(any(), any(), any()))
                .willThrow(new CouponExhaustedException("FULL"));

            mockMvc.perform(post("/api/coupons/FULL/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXHAUSTED"));
        }

        @Test
        void returns403_whenUserIsFromWrongCountry() throws Exception {
            given(couponService.useCoupon(any(), any(), any()))
                .willThrow(new CouponCountryMismatchException("PL-ONLY", "PL", "DE"));

            mockMvc.perform(post("/api/coupons/PL-ONLY/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COUPON_COUNTRY_MISMATCH"));
        }

        @Test
        void returns409_whenUserAlreadyUsedThisCoupon() throws Exception {
            given(couponService.useCoupon(any(), any(), any()))
                .willThrow(new CouponAlreadyUsedException("PROMO", "user-42"));

            mockMvc.perform(post("/api/coupons/PROMO/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_ALREADY_USED"));
        }

        @Test
        void returns503_whenGeolocationServiceIsDown() throws Exception {
            given(couponService.useCoupon(any(), any(), any()))
                .willThrow(new GeolocationServiceException("Service unreachable"));

            mockMvc.perform(post("/api/coupons/X/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("GEOLOCATION_UNAVAILABLE"));
        }

        @Test
        void returns400_whenUserIdIsBlank() throws Exception {
            mockMvc.perform(post("/api/coupons/X/use")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"userId":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='userId')]").isNotEmpty());
        }

        @Test
        void extractsClientIpFromXForwardedForHeader() throws Exception {
            Coupon coupon = Coupon.create("IP-TEST", 5, "PL");
            given(couponService.useCoupon(eq("IP-TEST"), eq("user-1"), eq("5.6.7.8")))
                .willReturn(coupon);

            mockMvc.perform(post("/api/coupons/IP-TEST/use")
                    .header("X-Forwarded-For", "5.6.7.8, 10.0.0.1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"userId":"user-1"}
                        """))
                .andExpect(status().isOk());
        }
    }
}
