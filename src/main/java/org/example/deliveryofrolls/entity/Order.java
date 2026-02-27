package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;   //Итоговая стоимость

    private String deliveryAddress;

    // Контактная информация
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String customerPhone;

    // Тип получения
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType; // DELIVERY, PICKUP

    //статус заказа
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    //способ оплаты
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @CreationTimestamp
    private LocalDateTime createdAt; // когда создан заказ

    @UpdateTimestamp
    private LocalDateTime updatedAt; // когда последний раз обновлялся

    private LocalDateTime deliveryTime; // желаемое время доставки

    private String notes; // комментарий к заказу

    public enum OrderStatus {
        PENDING,           // ожидает подтверждения
        CONFIRMED,         // подтвержден
        PREPARING,         // готовится
        READY_FOR_DELIVERY,// готов к выдаче
        ON_THE_WAY,        // в пути
        DELIVERED,         // доставлен
        COMPLETED,         // завершен
        CANCELLED          // отменен
    }

    public enum PaymentMethod {
        CASH,              // наличными
        CARD_ONLINE,       // картой онлайн
        CARD_ON_DELIVERY   // картой при получении
    }

    public enum DeliveryType {
        DELIVERY,          // доставка курьером
        PICKUP             // самовывоз
    }

}
