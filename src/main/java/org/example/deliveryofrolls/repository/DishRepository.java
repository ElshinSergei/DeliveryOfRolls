package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

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

}
