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

    @Column(nullable = false)
    private String dishName; // Сохраняем название на момент заказа

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal priceAtOrder; // цена на момент заказа

    private String specialInstructions;   // особые пожелания

    @Column(nullable = false)
    private BigDecimal totalPrice; // Сохраняем в БД для быстрых запросов

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (priceAtOrder != null) {
            this.totalPrice = priceAtOrder.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
