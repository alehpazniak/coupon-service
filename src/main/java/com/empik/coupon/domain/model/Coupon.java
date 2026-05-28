package com.empik.coupon.domain.model;

import com.empik.coupon.domain.exception.CouponExhaustedException;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "coupons",
    uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int maxUsages;

    @Column(nullable = false)
    private int currentUsages;

    @Column(nullable = false, length = 2)
    private String country;

    @Version
    private Long version;

    protected Coupon() {
        // required by JPA
    }

    public static Coupon create(String code, int maxUsages, String country) {
        var coupon = new Coupon();
        coupon.code = code.toUpperCase().trim();
        coupon.maxUsages = maxUsages;
        coupon.country = country.toUpperCase().trim();
        coupon.currentUsages = 0;
        coupon.createdAt = LocalDateTime.now();
        return coupon;
    }

    /**
     * Registers a single use of this coupon.
     * Must be called within a transaction that holds a pessimistic write lock on this entity.
     *
     * @throws CouponExhaustedException if the coupon has already reached its maximum usage count
     */
    public void use() {
        if (isExhausted()) {
            throw new CouponExhaustedException(this.code);
        }
        this.currentUsages++;
    }

    public boolean isExhausted() {
        return currentUsages >= maxUsages;
    }

    public boolean isAvailableForCountry(String countryCode) {
        return this.country.equalsIgnoreCase(countryCode);
    }

    public int remainingUsages() {
        return maxUsages - currentUsages;
    }

    public UUID getId()               { return id; }
    public String getCode()           { return code; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public int getMaxUsages()         { return maxUsages; }
    public int getCurrentUsages()     { return currentUsages; }
    public String getCountry()        { return country; }
}
