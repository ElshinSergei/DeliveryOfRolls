package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.repository.DishRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@CacheConfig(cacheNames = "dishes")
@Slf4j
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryService categoryService;

    // Получить все доступные блюда(только не удаленные)
    @Cacheable(key = "'all-available'")
    public List<Dish> getAllAvailableDishes() {
        return dishRepository.findAllAvailableWithIngredients();
    }

    // Найти блюдо по ID (только если не удалено)
    @Cacheable(key = "#id")
    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .filter(dish -> !dish.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));
    }

    // Найти блюдо даже если оно удалено
    public Dish getDishByIdIncludingDeleted(Long id) {
        return dishRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteDish(Long id) {
        Dish dish = getDishById(id);
        dish.setDeleted(true);  // мягкое удаление
        dish.setAvailable(false);
        dishRepository.save(dish);
    }

    // Восстановление
    @CacheEvict(allEntries = true)
    @Transactional
    public void restoreDish(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

        dish.setDeleted(false);
        dishRepository.save(dish);
    }

    // Фильтрация блюд
    public Page<Dish> findActiveDishes(String search, Long categoryId,
                                    Boolean available, Pageable pageable) {

        // Исключаем удаленные
        Specification<Dish> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("deleted"), false));

        // Фильтр по поиску (по названию или описанию)
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("name")), searchPattern),
                            cb.like(cb.lower(root.get("description")), searchPattern)
                    ));
        }
        // Фильтр по категории
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }
        // Фильтр по доступности
        if (available != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("available"), available));
        }
        return dishRepository.findAll(spec, pageable);
    }

    // Поиск в архиве (удаленные)
    public Page<Dish> findArchivedDishes(String search, Long categoryId, Pageable pageable) {
        Specification<Dish> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("deleted"), true));

        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), searchPattern));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        return dishRepository.findAll(spec, pageable);
    }

    // СОХРАНЕНИЕ (ДОБАВЛЕНИЕ ИЛИ ОБНОВЛЕНИЕ)
    @CacheEvict(value = "dishes", allEntries = true)
    @Transactional
    public Dish saveDishWithImage(Dish dish, MultipartFile file, String ingredientsString) {
        try {
            Dish dishToSave;

            if (dish.getId() != null) {
                // РЕДАКТИРОВАНИЕ - берем существующее блюдо из БД
                dishToSave = dishRepository.findById(dish.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

                // Обновляем поля
                dishToSave.setName(dish.getName());
                dishToSave.setDescription(dish.getDescription());
                dishToSave.setPrice(dish.getPrice());
                dishToSave.setWeight(dish.getWeight());
                dishToSave.setCalories(dish.getCalories());
                dishToSave.setAvailable(dish.isAvailable());

                // Обновляем категорию
                Long categoryId = dish.getCategory().getId();
                Category category = categoryService.getCategoryById(categoryId);
                dishToSave.setCategory(category);

                log.info("📝 Редактирование блюда ID: {}", dish.getId());

            } else {
                // НОВОЕ БЛЮДО - создаем новый объект
                dishToSave = new Dish();
                dishToSave.setName(dish.getName());
                dishToSave.setDescription(dish.getDescription());
                dishToSave.setPrice(dish.getPrice());
                dishToSave.setWeight(dish.getWeight());
                dishToSave.setCalories(dish.getCalories());
                dishToSave.setAvailable(dish.isAvailable());

                // Устанавливаем категорию
                Long categoryId = dish.getCategory().getId();
                Category category = categoryService.getCategoryById(categoryId);
                dishToSave.setCategory(category);

                log.info("➕ Добавление нового блюда");
            }

            // Обработка ингредиентов
            processIngredients(dishToSave, ingredientsString);

            // Обработка изображения
            processImage(dishToSave, file);

            // Сохраняем блюдо
            Dish savedDish = dishRepository.save(dishToSave);
            log.info("✅ Блюдо сохранено: {}", savedDish.getName());

            return savedDish;

        } catch (IOException e) {
            log.error("❌ Ошибка при загрузке файла", e);
            throw new RuntimeException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }
    }

    // Обработка ингредиентов
    private void processIngredients(Dish dish, String ingredientsString) {
        if (ingredientsString != null && !ingredientsString.isEmpty()) {
            List<String> ingredients = Arrays.stream(ingredientsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            dish.setIngredients(ingredients);
        } else {
            dish.setIngredients(new ArrayList<>());
        }
    }

    // Обработка изображения
    private void processImage(Dish dish, MultipartFile file) throws IOException {
        // ЕСЛИ ЗАГРУЖЕН НОВЫЙ ФАЙЛ
        if (file != null && !file.isEmpty()) {
            // Проверка типа файла
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Можно загружать только изображения");
            }
            // Проверка размера
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Файл слишком большой (макс. 5MB)");
            }
            // Удаляем старую картинку при редактировании
            deleteOldImageIfExists(dish);

            // Сохраняем новую картинку
            String fileName = saveImageFile(file);
            dish.setImageUrl("/uploads/dishes/" + fileName);
            log.info("Загружено новое изображение: {}", fileName);

        } else {
            // ЕСЛИ ФАЙЛ НЕ ЗАГРУЖЕН
            if (dish.getId() != null) {
                // Редактирование - сохраняем старую картинку
                Dish oldDish = dishRepository.findById(dish.getId()).orElse(null);
                if (oldDish != null) {
                    dish.setImageUrl(oldDish.getImageUrl());
                    log.info("Сохранено старое изображение: {}", oldDish.getImageUrl());
                }
            } else {
                // Новое блюдо без картинки - ставим заглушку
                dish.setImageUrl("/images/no-image.jpg");
                log.info("Установлено изображение-заглушка");
            }
        }
    }


    // Удаляем старую картинку при редактировании
    private void deleteOldImageIfExists(Dish dish) {
        if (dish.getId() != null) {
            Dish oldDish = dishRepository.findById(dish.getId()).orElse(null);
            if (oldDish != null && oldDish.getImageUrl() != null) {
                String oldFileName = oldDish.getImageUrl().replace("/uploads/dishes/", "");
                Path oldFilePath = Paths.get("/uploads/dishes/", oldFileName);
                try {
                    Files.deleteIfExists(oldFilePath);
                    log.info("Удален старый файл: {}", oldFileName);
                } catch (IOException e) {
                    log.error("Не удалось удалить старый файл: {}", oldFileName, e);
                }
            }
        }
    }

    private String saveImageFile(MultipartFile file) throws IOException {
        String uploadDir = "/uploads/dishes/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = originalFilename != null ?
                originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "dish.jpg";

        String fileName = System.currentTimeMillis() + "_" + safeFilename;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

}
