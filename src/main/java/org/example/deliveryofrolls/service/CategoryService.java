package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = "categories")
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Для админки - все категории
    @Cacheable(key = "'all'")
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllWithDishes();
    }

    // Для главной страницы - только доступные
    @Cacheable(key = "'available'")
    public List<Category> getAvailableCategories() {
        return categoryRepository.findByAvailableTrueOrderBySortOrderAsc();
    }

    // Получить категорию по ID
    @Cacheable(key = "#id")
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
    }

    // Сохранение
    @CacheEvict(allEntries = true)
    @Transactional
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // Переключение доступности
    @CacheEvict(allEntries = true)
    @Transactional
    public void toggleAvailability(Long id) {
        Category category = getCategoryById(id);
        category.setAvailable(!category.isAvailable());
        categoryRepository.save(category);
    }

    // Удаление (только если нет блюд)
    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (!category.getDishes().isEmpty()) {
            throw new RuntimeException("Нельзя удалить категорию с блюдами");
        }
        categoryRepository.delete(category);
    }
}
