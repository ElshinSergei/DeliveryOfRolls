package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;   //Итоговая стоимость

    @Embedded  // объект встраивается в таблицу orders, а не создает отдельную таблицу.
    private DeliveryInfo deliveryInfo;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;   //статус заказа

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;   //способ оплаты

    private String paymentStatus;   // "pending", "completed", "failed"
    private String transactionId;   // ID транзакции в платежной системе

    private LocalDateTime createdAt;    // когда создан заказ
    private LocalDateTime updatedAt;    // когда последний раз обновлялся
    private LocalDateTime deliveryTime; // желаемое время доставки

    private String notes; // комментарий к заказу

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryInfo {   // Информация о доставке
        // Адрес
        private String address;
        private String apartment;        // квартира
        private String entrance;         // подъезд
        private String floor;            // этаж
        private String intercom;         // домофон

        // Контактная информация
        private String customerName;
        private String customerPhone;

        // Тип получения
        @Enumerated(EnumType.STRING)
        private DeliveryType deliveryType; // DELIVERY, PICKUP
    }

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

    @PrePersist  // вызывается перед сохранением новой сущности
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate   // вызывается перед обновлением существующей
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
