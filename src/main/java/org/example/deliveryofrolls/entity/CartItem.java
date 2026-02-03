package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Min(value = 1, message = "Количество должно быть не менее 1")
    private int quantity;

    @ElementCollection   // Автоматически создает таблицу для хранения коллекции
    @CollectionTable(name = "cart_item_selected_options",
            joinColumns = @JoinColumn(name = "cart_item_id"))
    private List<SelectedOption> selectedOptions = new ArrayList<>();  // выбранные доп опции

    private String specialInstructions; // особые пожелания

    @Data
    @Embeddable  // встраивается в основную таблицу
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedOption {
        private Long optionId;
        private String optionName;
        private BigDecimal additionalPrice;
        private boolean selected;  // выбрана ли опция
    }

    // Цена 1 позиции в корзине
    public BigDecimal getTotalPrice() {
        // 1. Базовая цена блюда × количество
        BigDecimal basePrice = dish.getPrice().multiply(BigDecimal.valueOf(quantity));
        // 2. Цена выбранных опций × количество
        BigDecimal optionsPrice = selectedOptions.stream()
                .filter(SelectedOption::isSelected)   // только выбранные опции
                .map(SelectedOption::getAdditionalPrice)// берем цену каждой опции
                .reduce(BigDecimal.ZERO, BigDecimal::add) // суммируем
                .multiply(BigDecimal.valueOf(quantity)); // умножаем на количество блюд
        // 3. Итог = базовая цена + цена опций
        return basePrice.add(optionsPrice);
    }
}
