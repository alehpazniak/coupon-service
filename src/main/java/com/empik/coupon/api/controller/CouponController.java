package com.empik.coupon.api.controller;

import com.empik.coupon.api.dto.request.CreateCouponRequest;
import com.empik.coupon.api.dto.request.UseCouponRequest;
import com.empik.coupon.api.dto.response.CreateCouponResponse;
import com.empik.coupon.api.dto.response.UseCouponResponse;
import com.empik.coupon.api.util.IpExtractor;
import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
class CouponController {

    private final CouponService couponService;

    CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Creates a new discount coupon. No authentication required.
     *
     * @return 201 Created with the persisted coupon details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateCouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = couponService.createCoupon(
            request.code(),
            request.maxUsages(),
            request.country()
        );
        return CreateCouponResponse.from(coupon);
    }

    /**
     * Registers a single use of the coupon identified by {@code code} for the given user.
     *
     * <p>The country of the caller is resolved from their IP address. The request is rejected
     * if the country does not match the one configured on the coupon.
     *
     * @param code        coupon code (case-insensitive)
     * @param request     body containing the user identifier
     * @param httpRequest used to extract the client IP address
     * @return 200 OK with coupon code and remaining usages
     */
    @PostMapping("/{code}/use")
    UseCouponResponse useCoupon(
            @PathVariable String code,
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = IpExtractor.extractClientIp(httpRequest);
        Coupon coupon = couponService.useCoupon(code, request.userId(), clientIp);
        return UseCouponResponse.from(coupon);
    }
}
