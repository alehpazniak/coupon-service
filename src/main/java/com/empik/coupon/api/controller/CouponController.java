package com.empik.coupon.api.controller;

import com.empik.coupon.api.dto.request.CreateCouponRequest;
import com.empik.coupon.api.dto.request.UseCouponRequest;
import com.empik.coupon.api.dto.response.CreateCouponResponse;
import com.empik.coupon.api.dto.response.UseCouponResponse;
import com.empik.coupon.api.util.IpExtractor;
import com.empik.coupon.domain.service.CouponCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
class CouponController {

    private final CouponCommandService couponCommandService;
    private final IpExtractor ipExtractor;

    CouponController(CouponCommandService couponCommandService, IpExtractor ipExtractor) {
        this.couponCommandService = couponCommandService;
        this.ipExtractor = ipExtractor;
    }

    /**
     * Creates a new discount coupon. No authentication required.
     *
     * @return 201 Created with the persisted coupon details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateCouponResponse createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return CreateCouponResponse.from(
            couponCommandService.createCoupon(request.code(), request.maxUsages(), request.country())
        );
    }

    /**
     * Registers a single use of the coupon for the given user.
     *
     * <p>The caller's country is resolved from their IP address. The request is rejected
     * if the resolved country does not match the country configured on the coupon.
     *
     * @param code        coupon code (case-insensitive)
     * @param request     body containing the user identifier
     * @param httpRequest used to extract the originating IP address
     * @return 200 OK with coupon code and remaining usages
     */
    @PostMapping("/{code}/use")
    UseCouponResponse useCoupon(
            @PathVariable String code,
            @Valid @RequestBody UseCouponRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = ipExtractor.extractClientIp(httpRequest);
        return UseCouponResponse.from(
            couponCommandService.useCoupon(code, request.userId(), clientIp)
        );
    }
}
