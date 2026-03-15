package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // Получить только активные акции, отсортированные по порядку
    List<Promotion> findByActiveTrueOrderBySortOrderAsc();

    // Получить все акции с сортировкой
    List<Promotion> findAllByOrderBySortOrderAsc();
}
