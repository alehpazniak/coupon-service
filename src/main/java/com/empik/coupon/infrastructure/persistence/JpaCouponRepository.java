package com.empik.coupon.infrastructure.persistence;

import com.empik.coupon.domain.model.Coupon;
import com.empik.coupon.domain.port.CouponRepositoryPort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCouponRepository extends JpaRepository<Coupon, UUID>, CouponRepositoryPort {

    @Override
    Optional<Coupon> findByCode(String code);

    @Override
    boolean existsByCode(String code);

    /**
     * Fetches the coupon and immediately acquires a pessimistic write lock (SELECT … FOR UPDATE).
     *
     * <p>This ensures that concurrent transactions attempting to use the same coupon are
     * serialized at the database level, making the check-then-increment sequence atomic
     * without any application-level retry.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeWithLock(@Param("code") String code);
}
