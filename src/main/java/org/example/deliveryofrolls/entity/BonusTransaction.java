package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_transactions")
@Data
public class BonusTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private BonusAccount account;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order; // может быть null (для приветственных бонусов)

    @Column(nullable = false)
    private Integer amount; // положительное = начисление, отрицательное = списание

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        REGISTRATION,      // За регистрацию
        ORDER_EARN,        // Начисление за заказ
        ORDER_SPEND,       // Списание за заказ
        BIRTHDAY,          // За день рождения
        REFERRAL,          // За приглашенного друга
        REFUND,            // Возврат при отмене заказа
        ADMIN_ADJUSTMENT   // Ручная корректировка администратором
    }
}
