package com.empik.coupon.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error response body for all API error scenarios.
 *
 * <p>{@code fieldErrors} is omitted from the JSON payload when null (i.e. for non-validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    int status,
    String errorCode,
    String message,
    List<FieldError> fieldErrors,
    Instant timestamp
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String errorCode, String message) {
        return new ApiError(status, errorCode, message, null, Instant.now());
    }

    public static ApiError ofValidation(int status, List<FieldError> fieldErrors) {
        return new ApiError(status, "VALIDATION_ERROR", "Request validation failed", fieldErrors, Instant.now());
    }
}
