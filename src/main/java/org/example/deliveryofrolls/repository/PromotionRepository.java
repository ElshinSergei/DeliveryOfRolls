package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Найти по коду
    Optional<Promotion> findByCode(String code);

    // Найти активные промокоды
    List<Promotion> findByActiveTrue();

}
