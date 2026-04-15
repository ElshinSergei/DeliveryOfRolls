package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.PromoCodeResponse;
import org.example.deliveryofrolls.entity.PromoCode;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.PromoCodeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final UserService userService;

    /**
     * Основной метод - проверка и применение промокода
     *
     * @param code        промокод который ввел пользователь (например "WELCOME10")
     * @param orderAmount сумма заказа до скидки
     * @return ответ с результатом проверки
     */
    @Transactional
    public PromoCodeResponse applyPromoCode(String code, BigDecimal orderAmount, UserDetails userDetails) {

        // Шаг 1: Проверяем, что код не пустой
        if (code == null || code.trim().isEmpty()) {
            log.warn("Пользователь ввел пустой промокод");
            return PromoCodeResponse.error("Введите промокод");
        }

        // Шаг 2: Приводим к верхнему регистру и убираем пробелы
        String upperCode = code.trim().toUpperCase();
        log.info("Проверка промокода: {}, сумма заказа: {}", upperCode, orderAmount);

        // Шаг 3: Ищем промокод в базе данных
        PromoCode promoCode = promoCodeRepository.findByCode(upperCode).orElse(null);

        // Шаг 4: Если промокод не найден в БД
        if (promoCode == null) {
            log.warn("Промокод {} не найден в базе данных", upperCode);
            return PromoCodeResponse.error("Промокод не найден");
        }

        // ========== ПОЛУЧАЕМ ID ПОЛЬЗОВАТЕЛЯ ==========
        Long userId = null;
        if (userDetails != null) {
            try {
                User user = userService.getCurrentUser(userDetails);
                userId = user.getId();
            } catch (Exception e) {
                log.warn("Не удалось получить пользователя: {}", e.getMessage());
            }
        }

        // Проверяем, может ли пользователь использовать промокод
        if (!promoCode.canBeUsedByUser(userId)) {
            if (promoCode.getUsageType() == PromoCode.UsageType.SINGLE_PER_USER) {
                if (userId == null) {
                    return PromoCodeResponse.error("Для использования этого промокода необходимо авторизоваться");
                }
                return PromoCodeResponse.error("Вы уже использовали этот промокод");
            } else {
                return PromoCodeResponse.error("Лимит использований промокода исчерпан");
            }
        }

        // Шаг 5: Проверяем, активен ли промокод и не истек ли срок
        if (!promoCode.isValid()) {
            log.warn("Промокод {} недействителен", upperCode);

            // Пытаемся понять причину
            LocalDateTime now = LocalDateTime.now();

            if (!promoCode.isActive()) {
                return PromoCodeResponse.error("Промокод деактивирован");
            }
            if (promoCode.getUsedCount() >= promoCode.getUsageLimit()) {
                return PromoCodeResponse.error("Промокод уже использован");
            }
            if (now.isBefore(promoCode.getValidFrom())) {
                return PromoCodeResponse.error("Промокод еще не начал действовать");
            }
            if (now.isAfter(promoCode.getValidUntil())) {
                return PromoCodeResponse.error("Срок действия промокода истек");
            }

            return PromoCodeResponse.error("Промокод недействителен");
        }

        // Шаг 6: Проверяем минимальную сумму (сравниваем BigDecimal)
        BigDecimal minOrder = BigDecimal.valueOf(promoCode.getMinOrderAmount());
        if (orderAmount.compareTo(minOrder) < 0) {
            String message = String.format(
                    "Минимальная сумма заказа для этого промокода: %d ₽",
                    promoCode.getMinOrderAmount()
            );
            log.warn("Сумма заказа {} меньше минимальной {}", orderAmount, minOrder);
            return PromoCodeResponse.error(message);
        }

        // Шаг 7: Рассчитываем скидку
        BigDecimal discount = calculateDiscount(promoCode, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);

        // Шаг 8: Форматируем текст скидки
        String discountText = formatDiscountText(promoCode);

        log.info("✅ Промокод {} применен: скидка {} ₽, итог {} ₽",
                upperCode, discount, finalAmount);

        // ========== СОХРАНЯЕМ ИСПОЛЬЗОВАНИЕ ==========
        promoCode.markAsUsed(userId);
        promoCodeRepository.save(promoCode);
        // ============================================

        return PromoCodeResponse.success(
                upperCode,
                discount,
                finalAmount,
                discountText
        );
    }

    /**
     * Совместимость со старым методом (без пользователя)
     */
    @Transactional
    public PromoCodeResponse applyPromoCode(String code, BigDecimal orderAmount) {
        return applyPromoCode(code, orderAmount, null);
    }

    /**
     * Рассчет скидки с BigDecimal
     */
    private BigDecimal calculateDiscount(PromoCode promoCode, BigDecimal orderAmount) {
        BigDecimal discount;

        if (promoCode.getDiscountType() == PromoCode.DiscountType.PERCENTAGE) {
            // Процентная скидка: сумма * процент / 100
            BigDecimal percent = BigDecimal.valueOf(promoCode.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(percent);

            // Проверяем максимальную скидку
            if (promoCode.getMaxDiscount() != null) {
                BigDecimal maxDiscount = BigDecimal.valueOf(promoCode.getMaxDiscount());
                if (discount.compareTo(maxDiscount) > 0) {
                    discount = maxDiscount;
                }
            }
        } else {
            // Фиксированная скидка
            discount = BigDecimal.valueOf(promoCode.getDiscountValue());

            // Скидка не может быть больше суммы заказа
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
        }

        // Округляем до целых рублей
        return discount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Форматирование текста скидки
     */
    private String formatDiscountText(PromoCode promoCode) {
        if (promoCode.getDiscountType() == PromoCode.DiscountType.PERCENTAGE) {
            String text = String.format("%d%%", promoCode.getDiscountValue());
            if (promoCode.getMaxDiscount() != null) {
                text += String.format(" (макс. %d ₽)", promoCode.getMaxDiscount());
            }
            return text;
        } else {
            return String.format("%d ₽", promoCode.getDiscountValue());
        }
    }

    /**
     * Отметить промокод как использованный
     */
    @Transactional
    public void usePromoCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }

        String upperCode = code.trim().toUpperCase();
        promoCodeRepository.findByCode(upperCode).ifPresent(promoCode -> {
            promoCode.setUsedCount(promoCode.getUsedCount() + 1);
            promoCodeRepository.save(promoCode);
            log.info("📊 Промокод {} использован. Всего использований: {}",
                    upperCode, promoCode.getUsedCount());
        });
    }

    // ========== МЕТОДЫ ДЛЯ CRUD ==========

    @Transactional(readOnly = true)
    public List<PromoCode> findAll() {
        return promoCodeRepository.findAllByOrderByValidFromDesc();
    }

    @Transactional(readOnly = true)
    public PromoCode findById(Long id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Промокод не найден"));
    }

    public PromoCode save(PromoCode promoCode) {
        if (promoCode.getValidFrom().isAfter(promoCode.getValidUntil())) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (promoCode.getDiscountType() == PromoCode.DiscountType.PERCENTAGE) {
            if (promoCode.getDiscountValue() <= 0 || promoCode.getDiscountValue() > 100) {
                throw new IllegalArgumentException("Процент скидки должен быть от 1 до 100");
            }
        }

        if (promoCode.getDiscountType() == PromoCode.DiscountType.FIXED) {
            if (promoCode.getDiscountValue() <= 0) {
                throw new IllegalArgumentException("Сумма скидки должна быть больше 0");
            }
        }

        // Для SINGLE_PER_USER usageLimit должен быть 1
        if (promoCode.getUsageType() == PromoCode.UsageType.SINGLE_PER_USER) {
            promoCode.setUsageLimit(1);
        }


        PromoCode saved = promoCodeRepository.save(promoCode);
        log.info("Промокод сохранен: {}", saved.getCode());
        return saved;
    }

    public void delete(Long id) {
        PromoCode promoCode = findById(id);
        promoCodeRepository.delete(promoCode);
        log.info("Промокод удален: {}", promoCode.getCode());
    }

    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return promoCodeRepository.existsByCode(code);
    }

    public PromoCode duplicate(Long id) {
        PromoCode original = findById(id);

        PromoCode duplicate = new PromoCode();
        duplicate.setCode(original.getCode() + "_COPY");
        duplicate.setDiscountType(original.getDiscountType());
        duplicate.setDiscountValue(original.getDiscountValue());
        duplicate.setMinOrderAmount(original.getMinOrderAmount());
        duplicate.setMaxDiscount(original.getMaxDiscount());
        duplicate.setValidFrom(LocalDateTime.now());
        duplicate.setValidUntil(LocalDateTime.now().plusDays(30));
        duplicate.setUsageLimit(original.getUsageLimit());
        duplicate.setUsedCount(0);
        duplicate.setActive(true);
        duplicate.setDescription(original.getDescription() + " (копия)");

        return promoCodeRepository.save(duplicate);
    }

    public boolean toggleActive(Long id) {
        PromoCode promoCode = findById(id);
        promoCode.setActive(!promoCode.isActive());
        promoCodeRepository.save(promoCode);
        return promoCode.isActive();
    }

    // ========== НОВЫЕ МЕТОДЫ ДЛЯ DASHBOARD ==========

    /**
     * Получить общее количество промокодов
     */
    @Transactional(readOnly = true)
    public long count() {
        return promoCodeRepository.count();
    }

    /**
     * Получить количество активных промокодов
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return promoCodeRepository.countByActiveTrueAndValidUntilAfter(LocalDateTime.now());
    }

    /**
     * Получить количество истекших промокодов
     */
    @Transactional(readOnly = true)
    public long countExpired() {
        return promoCodeRepository.countByValidUntilBefore(LocalDateTime.now());
    }

    /**
     * Получить общее количество использований промокодов
     */
    @Transactional(readOnly = true)
    public int getTotalUsageCount() {
        return promoCodeRepository.sumUsedCount();
    }

    /**
     * Получить самые популярные промокоды
     */
    @Transactional(readOnly = true)
    public List<PromoCode> findMostUsed(int limit) {
        return promoCodeRepository.findTopByOrderByUsedCountDesc(limit);
    }

    /**
     * Получить последние N промокодов
     */
    @Transactional(readOnly = true)
    public List<PromoCode> findRecent(int limit) {
        return promoCodeRepository.findTopByOrderByCreatedAtDesc(limit);
    }

    /**
     * Получить статистику по промокодам (для dashboard)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", count());
        stats.put("active", countActive());
        stats.put("expired", countExpired());
        stats.put("totalUsage", getTotalUsageCount());
        stats.put("popular", findMostUsed(5));
        stats.put("recent", findRecent(5));
        return stats;
    }

    public boolean existsByCodeAndIdNot(String code, Long id) {
        if (id == null) {
            return promoCodeRepository.existsByCode(code);
        }
        return promoCodeRepository.existsByCodeAndIdNot(code, id);
    }
}
