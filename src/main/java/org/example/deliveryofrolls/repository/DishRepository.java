package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    // Для всех доступных блюд. С FETCH (одним запросом)
    @Query("SELECT DISTINCT d FROM Dish d " +
            "LEFT JOIN FETCH d.ingredients " +
            "WHERE d.available = true " +
            "ORDER BY d.name")
    List<Dish> findAllAvailableWithIngredients();

    // Для блюд по категории. С FETCH (одним запросом)
    @Query("SELECT DISTINCT d FROM Dish d " +
            "LEFT JOIN FETCH d.ingredients " +
            "WHERE d.available = true AND d.category.id = :categoryId " +
            "ORDER BY d.name")
    List<Dish> findByCategoryIdWithIngredients(@Param("categoryId") Long categoryId);

    List<Dish> findAllByOrderByName();

    // Найти все доступные блюда
    List<Dish> findByAvailableTrue();

    // Найти по категории
    List<Dish> findByCategory(Category category);

    // Найти по ID категории
    List<Dish> findByCategoryId(Long categoryId);

    // Поиск по названию
    List<Dish> findByNameContainingIgnoreCase(String name);

    // Сортировка по цене (возрастание)
    List<Dish> findAllByOrderByPriceAsc();

    // Сортировка по цене (убывание)
    List<Dish> findAllByOrderByPriceDesc();

    // Кол-во доступных блюд
    Long countByAvailableTrue();

}
