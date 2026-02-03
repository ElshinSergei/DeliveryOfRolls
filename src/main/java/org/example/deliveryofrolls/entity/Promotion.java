package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private PromotionType type; // PERCENT, FIXED_AMOUNT, FREE_DELIVERY

    private BigDecimal discountValue; // процент или фиксированная сумма
    private BigDecimal minimumOrderAmount; // минимальная сумма заказа

    // Период действия акции
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Integer usageLimit;     // лимит использований
    private Integer usedCount = 0;  // cколько раз уже использовали

    private boolean active = true;   // Включена/выключена акция

    public enum PromotionType {
        PERCENT,           // Процентная скидка
        FIXED_AMOUNT,      // Фиксированная сумма
        FREE_DELIVERY      // Бесплатная доставка
    }
}
