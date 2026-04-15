package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "promo_codes")
@Data
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value")
    private Integer discountValue; // 10 или 100

    // ========== ОГРАНИЧЕНИЯ ==========
    @Column(name = "min_order_amount")
    private Integer minOrderAmount = 0;  // Минимальная сумма заказа (по умолчанию 0)

    @Column(name = "max_discount")
    private Integer maxDiscount;        // Максимальная скидка (только для процентных)

    // ========== СРОКИ ДЕЙСТВИЯ ==========
    @Column(name = "valid_from")
    private LocalDateTime validFrom;    // С какого числа действует

    @Column(name = "valid_until")
    private LocalDateTime validUntil;   // До какого числа действует

    // ========== ЛИМИТЫ ИСПОЛЬЗОВАНИЯ ==========
    @Column(name = "usage_limit")
    private Integer usageLimit = 100;   // Сколько раз можно использовать (по умолчанию 100)

    @Enumerated(EnumType.STRING)
    private UsageType usageType = UsageType.MULTIPLE;

    @Column(name = "used_count")
    private Integer usedCount = 0;   // Сколько раз уже использовали

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promo_code_users",
            joinColumns = @JoinColumn(name = "promo_code_id"))
    @Column(name = "user_id")
    private Set<Long> usedByUsers = new HashSet<>();

    @Column(name = "is_active")
    private boolean isActive = true;   // Активен ли промокод (можно отключить вручную)

    private String description;       // Описание для пользователя

    // ========== ПОЛЯ ДЛЯ СЧАСТЛИВЫХ ЧАСОВ ==========

    @Column(name = "has_time_restriction")
    private boolean hasTimeRestriction = false;  // Есть ли ограничение по времени

    @Column(name = "time_start")
    private String timeStart;  // Начало "счастливых часов"

    @Column(name = "time_end")
    private String timeEnd;    // Конец "счастливых часов"

    @Column(name = "days_of_week")
    private String daysOfWeek; // Дни недели

    @Column(name = "timezone")
    private String timezone = "Europe/Moscow";

    public enum DiscountType {
        PERCENTAGE,      // Процентная скидка (10%, 15%)
        FIXED           // Фиксированная скидка (100₽, 200₽)
    }

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public enum UsageType {
        SINGLE_PER_USER,  // Один раз на пользователя
        MULTIPLE          // Много раз (общий лимит)
    }

    /**
     * Проверяет, действителен ли промокод
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        // Базовая проверка (активен, лимиты, срок действия)
        if (!isActive || usedCount >= usageLimit ||
                now.isBefore(validFrom) || now.isAfter(validUntil)) {
            return false;
        }
        // Если есть ограничение по времени - проверяем
        if (hasTimeRestriction) {
            return isWithinHappyHours(now);
        }

        return true;
    }

    /**
     * Рассчитывает сумму скидки
     * @param orderAmount сумма заказа
     * @return сумма скидки
     */
    public int calculateDiscount(int orderAmount) {
        int discount = 0;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount * discountValue / 100;
            if (maxDiscount != null && discount > maxDiscount) {
                discount = maxDiscount;
            }
        } else {
            discount = discountValue;
            if (discount > orderAmount) {
                discount = orderAmount;
            }
        }
        return discount;
    }

    /**
     * Проверяет, попадает ли текущее время в "счастливые часы"
     */
    private boolean isWithinHappyHours(LocalDateTime dateTime) {
        if (timeStart == null || timeEnd == null) {
            return false;
        }
        // Проверяем день недели
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            String currentDay = dateTime.getDayOfWeek().toString();
            List<String> allowedDays = Arrays.asList(daysOfWeek.split(","));
            if (!allowedDays.contains(currentDay)) {
                return false; // Сегодня не день акции
            }
        }
        // Проверяем время
        LocalTime currentTime = dateTime.toLocalTime();
        LocalTime start = LocalTime.parse(timeStart);
        LocalTime end = LocalTime.parse(timeEnd);
        // Обрабатываем случай, когда акция идет через полночь (23:00 - 02:00)
        if (end.isBefore(start)) {
            // Акция переходит через полночь
            return currentTime.isAfter(start) || currentTime.isBefore(end);
        } else {
            // Обычная акция в течение дня
            return !currentTime.isBefore(start) && !currentTime.isAfter(end);
        }
    }

    /**
     * Проверяет, может ли пользователь использовать промокод
     */
    public boolean canBeUsedByUser(Long userId) {
        if (!isValid()) {
            return false;
        }

        if (usageType == UsageType.SINGLE_PER_USER) {
            if (userId == null) {
                return false;
            }
            return !usedByUsers.contains(userId);
        }

        return true;
    }

    /**
     * Отметить использование промокода
     */
    public void markAsUsed(Long userId) {
        if (usageType == UsageType.SINGLE_PER_USER && userId != null) {
            usedByUsers.add(userId);
        }
        usedCount++;
    }
}
