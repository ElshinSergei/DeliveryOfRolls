package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    private int quantity;
    private BigDecimal priceAtOrder; // цена на момент заказа

    @ElementCollection  // Автоматически создает таблицу для хранения коллекции
    @CollectionTable(name = "order_item_options",
            joinColumns = @JoinColumn(name = "order_item_id"))
    private List<SelectedOption> selectedOptions = new ArrayList<>();   // выбранные доп опции

    private String specialInstructions;   // особые пожелания

    @Data
    @Embeddable   // встраивается в основную таблицу
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedOption {
        private String optionName;
        private BigDecimal additionalPrice;
    }

    // Цена позиции в заказе
    public BigDecimal getTotalPrice() {
        // 1. Базовая цена блюда × количество
        BigDecimal basePrice = priceAtOrder.multiply(BigDecimal.valueOf(quantity));
        // 2. Цена всех выбранных опций × количество
        BigDecimal optionsPrice = selectedOptions.stream()
                .map(SelectedOption::getAdditionalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(quantity));
        // 3. Итог = базовая цена + цена опций
        return basePrice.add(optionsPrice);
    }
}
