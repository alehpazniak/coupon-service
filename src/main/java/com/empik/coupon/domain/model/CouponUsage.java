package com.empik.coupon.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "coupon_usages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_coupon_usages_coupon_user",
        columnNames = {"coupon_id", "user_id"}
    )
)
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime usedAt;

    protected CouponUsage() {
        // required by JPA
    }

    public static CouponUsage of(Coupon coupon, String userId) {
        var usage = new CouponUsage();
        usage.coupon = coupon;
        usage.userId = userId;
        usage.usedAt = LocalDateTime.now();
        return usage;
    }

    public UUID getId()              { return id; }
    public Coupon getCoupon()        { return coupon; }
    public String getUserId()        { return userId; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
