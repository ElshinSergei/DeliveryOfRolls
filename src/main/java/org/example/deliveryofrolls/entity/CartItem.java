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

    private String specialInstructions; // особые пожелания

    private BigDecimal priceAtTime; // Цена на момент добавления

    // Цена 1 позиции в корзине
    public BigDecimal getTotalPrice() {

        // Проверка на null
        if (dish == null || dish.getPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentPrice = (priceAtTime != null) ? priceAtTime : dish.getPrice();

        // Базовая цена блюда × количество
        BigDecimal basePrice = currentPrice.multiply(BigDecimal.valueOf(quantity));

        return basePrice;
    }
}
