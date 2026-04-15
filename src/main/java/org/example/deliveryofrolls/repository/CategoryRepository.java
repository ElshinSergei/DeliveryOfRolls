package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Получить все категории, отсортированные по sortOrder
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.dishes ORDER BY c.sortOrder")
    List<Category> findAllWithDishes();

    // Получить только доступные категории, отсортированные по sortOrder
    List<Category> findByAvailableTrueOrderBySortOrderAsc();

    Optional<Category> findByName(String name);

}
