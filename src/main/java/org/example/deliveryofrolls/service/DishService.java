package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;

    // Получить все доступные блюда
    public List<Dish> getAllAvailableDishes() {
        return dishRepository.findAllAvailableWithIngredients();
    }

    // Получить блюда по категории
    public List<Dish> getDishesByCategory(Long categoryId) {
        return dishRepository.findByCategoryIdWithIngredients(categoryId);
    }

    // Найти блюдо по ID
    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));
    }

    // Поиск блюд по названию
    public List<Dish> searchDishes(String query) {
        return dishRepository.findByNameContainingIgnoreCase(query);
    }

    // Получить все категории
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    // Получить категорию по ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
    }


    // Методы для админки
    @Transactional
    public Dish createDish(Dish dish) {
        return dishRepository.save(dish);
    }

    @Transactional
    public Dish updateDish(Long id, Dish dishDetails) {
        Dish dish = getDishById(id);
        dish.setName(dishDetails.getName());
        dish.setDescription(dishDetails.getDescription());
        dish.setPrice(dishDetails.getPrice());
        dish.setCategory(dishDetails.getCategory());
        dish.setAvailable(dishDetails.isAvailable());
        dish.setWeight(dishDetails.getWeight());
        dish.setCalories(dishDetails.getCalories());

        return dishRepository.save(dish);
    }

    @Transactional
    public void deleteDish(Long id) {
        Dish dish = getDishById(id);
        dish.setAvailable(false); // мягкое удаление
        dishRepository.save(dish);
    }

}
