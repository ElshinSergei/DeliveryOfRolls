package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.PromoCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    // Найти промокод по его коду
    Optional<PromoCode> findByCode(String code);

    boolean existsByCode(String code);

    List<PromoCode> findAllByOrderByValidFromDesc();

    // Для dashboard
    @Query("SELECT COUNT(p) FROM PromoCode p WHERE p.isActive = true AND p.validUntil > :now")
    long countByActiveTrueAndValidUntilAfter(@Param("now") LocalDateTime now);

    long countByValidUntilBefore(LocalDateTime date);

    @Query("SELECT COALESCE(SUM(p.usedCount), 0) FROM PromoCode p")
    int sumUsedCount();

    @Query(value = "SELECT * FROM promo_codes ORDER BY used_count DESC LIMIT :limit", nativeQuery = true)
    List<PromoCode> findTopByOrderByUsedCountDesc(@Param("limit") int limit);

    @Query(value = "SELECT * FROM promo_codes ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<PromoCode> findTopByOrderByCreatedAtDesc(@Param("limit") int limit);

    @Query(value = "SELECT DATE(o.created_at), COUNT(*), COALESCE(SUM(o.discount_amount), 0) " +
            "FROM orders o " +
            "WHERE o.promo_code IS NOT NULL " +
            "AND o.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.created_at) " +
            "ORDER BY DATE(o.created_at)", nativeQuery = true)
    List<Object[]> getDailyUsageStats(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    boolean existsByCodeAndIdNot(String code, Long id);
}
