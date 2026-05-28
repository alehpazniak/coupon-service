package com.empik.coupon.api.exception;

import com.empik.coupon.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CouponNotFoundException.class)
    ResponseEntity<ApiError> handleCouponNotFound(CouponNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.of(404, "COUPON_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CouponExhaustedException.class)
    ResponseEntity<ApiError> handleCouponExhausted(CouponExhaustedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "COUPON_EXHAUSTED", ex.getMessage()));
    }

    @ExceptionHandler(CouponCountryMismatchException.class)
    ResponseEntity<ApiError> handleCountryMismatch(CouponCountryMismatchException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError.of(403, "COUPON_COUNTRY_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(CouponAlreadyUsedException.class)
    ResponseEntity<ApiError> handleAlreadyUsed(CouponAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "COUPON_ALREADY_USED", ex.getMessage()));
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    ResponseEntity<ApiError> handleAlreadyExists(CouponAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError.of(409, "COUPON_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(GeolocationServiceException.class)
    ResponseEntity<ApiError> handleGeolocationError(GeolocationServiceException ex) {
        log.error("Geolocation service error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiError.of(503, "GEOLOCATION_UNAVAILABLE",
                "Unable to determine your location. Please try again later."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.ofValidation(400, fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(400, "MALFORMED_REQUEST", "Request body is missing or cannot be parsed"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
