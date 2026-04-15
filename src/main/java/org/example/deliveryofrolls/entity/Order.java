package org.example.deliveryofrolls.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice;   //Итоговая стоимость

    private String deliveryAddress;

    // ========== НОВЫЕ ПОЛЯ ДЛЯ ДЕТАЛЕЙ АДРЕСА ==========
    @Column(name = "delivery_entrance", length = 10)
    private String deliveryEntrance;   // подъезд

    @Column(name = "delivery_floor", length = 10)
    private String deliveryFloor;      // этаж

    @Column(name = "delivery_apartment", length = 10)
    private String deliveryApartment;  // квартира

    @Column(name = "delivery_intercom", length = 20)
    private String deliveryIntercom;   // домофон
    // ================================================

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

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "bonus_used")
    private Integer bonusUsed = 0;  // Сколько бонусов потратил

    @Column(name = "bonus_earned")
    private Integer bonusEarned = 0;  // Сколько бонусов начислено

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PREPARING,
        READY_FOR_DELIVERY,
        ON_THE_WAY,
        DELIVERED,
        COMPLETED,
        CANCELLED;

        public String getDisplayName() {
            switch(this) {
                case PENDING: return "Ожидает";
                case CONFIRMED: return "Подтвержден";
                case PREPARING: return "Готовится";
                case READY_FOR_DELIVERY: return "Готов к выдаче";
                case ON_THE_WAY: return "В пути";
                case DELIVERED: return "Доставлен";
                case COMPLETED: return "Завершен";
                case CANCELLED: return "Отменен";
                default: return this.name();
            }
        }
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
