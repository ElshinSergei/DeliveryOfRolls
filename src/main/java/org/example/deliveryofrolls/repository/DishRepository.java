package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long>, JpaSpecificationExecutor<Dish> {

    // Для всех доступных блюд. С FETCH (одним запросом)
    @Query("SELECT DISTINCT d FROM Dish d " +
            "LEFT JOIN FETCH d.ingredients " +
            "WHERE d.available = true AND d.deleted = false " +
            "ORDER BY d.name")
    List<Dish> findAllAvailableWithIngredients();

    // Для блюд по категории. С FETCH (одним запросом)
    @Query("SELECT DISTINCT d FROM Dish d " +
            "LEFT JOIN FETCH d.ingredients " +
            "WHERE d.available = true AND d.category.id = :categoryId AND d.deleted = false " +
            "ORDER BY d.name")
    List<Dish> findByCategoryIdWithIngredients(@Param("categoryId") Long categoryId);

    // Поиск по названию
    List<Dish> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    // Кол-во доступных блюд
    Long countByAvailableTrueAndDeletedFalse();


    // Для админки (показываем ВСЕ, включая удаленные)
    @Query("SELECT d FROM Dish d ORDER BY d.name")
    List<Dish> findAllIncludingDeleted();

    @Query("SELECT d FROM Dish d WHERE d.id = :id")
    Optional<Dish> findByIdIncludingDeleted(@Param("id") Long id);

    // Удаленные блюда
    List<Dish> findByDeletedTrue();

    List<Dish> findByCategoryAndAvailableTrue(Category category);
}
