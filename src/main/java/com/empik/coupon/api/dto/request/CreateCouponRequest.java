package com.empik.coupon.api.dto.request;

import jakarta.validation.constraints.*;

public record CreateCouponRequest(

    @NotBlank(message = "Coupon code must not be blank")
    @Size(min = 1, max = 50, message = "Coupon code length must be between 1 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Coupon code may only contain letters, digits, hyphens and underscores")
    String code,

    @Min(value = 1, message = "Max usages must be at least 1")
    @Max(value = 1_000_000, message = "Max usages must not exceed 1,000,000")
    int maxUsages,

    @NotBlank(message = "Country must not be blank")
    @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO 3166-1 alpha-2 code")
    @Pattern(regexp = "[A-Za-z]{2}", message = "Country must consist of two letters only")
    String country
) {}
