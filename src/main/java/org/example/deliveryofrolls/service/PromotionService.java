package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Promotion;
import org.example.deliveryofrolls.repository.PromotionRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "promotions")
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;

    @Cacheable(key = "'all'")
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAllByOrderBySortOrderAsc();
    }

    // Получить только активные акции (для клиентской части)
    @Cacheable(key = "'active'")
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @Cacheable(key = "#id")
    public Promotion getById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Акция не найдена с id: " + id));
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void save(Promotion promotion) {
        promotionRepository.save(promotion);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void delete(Long id) {

        Promotion promotion = getById(id);
        if (promotion.getImageUrl() != null) {
            deleteOldImageIfExists(promotion);
        }
        promotionRepository.deleteById(id);
        log.info("Акция удалена: id={}", id);
    }

    // Сохранение
    @CacheEvict(allEntries = true)
    public Promotion savePromotionWithImage(Promotion promotion, MultipartFile file) {
        try {
            // Определяем, новая это акция или редактирование
            boolean isNewPromotion = (promotion.getId() == null);

            // Проверка наличия файла для новой акции
            if (isNewPromotion && file.isEmpty()) {
                throw new IllegalArgumentException("Выберите файл изображения");
            }

            // Если файл загружен - обрабатываем его
            if (!file.isEmpty()) {
                // Проверка типа файла
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("Можно загружать только изображения");
                }

                // Проверка размера (5MB максимум)
                if (file.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("Файл слишком большой (макс. 5MB)");
                }

                if (!isNewPromotion) {
                    Promotion existingPromotion = getById(promotion.getId());
                    if (existingPromotion.getImageUrl() != null) {
                        deleteOldImageIfExists(existingPromotion);
                    }
                }

                // Очистка имени файла от опасных символов
                String originalFilename = file.getOriginalFilename();
                String safeFilename = originalFilename != null ?
                        originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "image.jpg";

                // Генерация уникального имени с timestamp
                String fileName = System.currentTimeMillis() + "_" + safeFilename;

                // Определяем папку для сохранения
                String uploadDir = "/uploads/promotions/";

                // Создание директории
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Сохранение файла с заменой существующего
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Установка URL для доступа через браузер
                promotion.setImageUrl("/uploads/promotions/" + fileName);

            } else if (!isNewPromotion) {
                // Если это редактирование и файл не загружен,
                // сохраняем старый URL
                Promotion existingPromotion = getById(promotion.getId());
                promotion.setImageUrl(existingPromotion.getImageUrl());
            }

            // Сохраняем акцию
            return promotionRepository.save(promotion);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении файла: " + e.getMessage(), e);
        }
    }

    /**
     * Получить общее количество акций
     */
    @Cacheable(key = "'count'")
    @Transactional(readOnly = true)
    public long count() {
        return promotionRepository.count();
    }

    /**
     * Получить количество активных акций
     */
    @Cacheable(key = "'activeCount'")
    @Transactional(readOnly = true)
    public long countActive() {
        return promotionRepository.countByActiveTrue();
    }

    /**
     * Получить последние N акций (для dashboard)
     */
    @Cacheable(key = "'recent-' + #limit")
    @Transactional(readOnly = true)
    public List<Promotion> findRecent(int limit) {
        return promotionRepository.findTopByOrderByCreatedAtDesc(limit);
    }

    /**
     * Получить статистику по акциям (для dashboard)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", count());
        stats.put("active", countActive());
        stats.put("inactive", count() - countActive());

        // Последние 5 акций
        stats.put("recent", findRecent(5));

        return stats;
    }

    private void deleteOldImageIfExists(Promotion promotion) {
        if (promotion.getImageUrl() != null) {
            String oldFileName = promotion.getImageUrl().replace("/uploads/promotions/", "");
            Path oldFilePath = Paths.get("/uploads/promotions/", oldFileName);
            try {
                Files.deleteIfExists(oldFilePath);
                log.info("Удален старый файл акции: {}", oldFileName);
            } catch (IOException e) {
                log.error("Не удалось удалить старый файл акции: {}", oldFileName, e);
            }
        }
    }
}

