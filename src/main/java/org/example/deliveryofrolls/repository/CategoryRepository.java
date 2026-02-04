package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Найти по названию
    Optional<Category> findByName(String name);

    // Проверить существование по названию
    boolean existsByName(String name);

    // Сортировка по порядку
    List<Category> findAllByOrderBySortOrderAsc();


}
