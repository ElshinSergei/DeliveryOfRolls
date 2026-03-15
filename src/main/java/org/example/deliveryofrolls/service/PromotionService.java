package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "promotions")
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
        promotionRepository.deleteById(id);
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

                // Очистка имени файла от опасных символов
                String originalFilename = file.getOriginalFilename();
                String safeFilename = originalFilename != null ?
                        originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "image.jpg";

                // Генерация уникального имени с timestamp
                String fileName = System.currentTimeMillis() + "_" + safeFilename;

                // Определяем папку для сохранения
                String uploadDir = System.getProperty("user.home") + "/sushi-uploads/promotions/";

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

}
