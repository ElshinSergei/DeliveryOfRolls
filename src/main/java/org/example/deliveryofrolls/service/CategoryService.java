package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Получить все категории
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Получить категорию по ID
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
    }

    // Получить категорию по названию
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
    }

    // Получить отсортированные категории
    public List<Category> getAllCategoriesSorted() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

}
