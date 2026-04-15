package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "bonus_settings")
@Data
public class BonusSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "earn_percent")
    private Integer earnPercent = 5; // % начисления от суммы заказа

    @Column(name = "max_spend_percent")
    private Integer maxSpendPercent = 30; // максимум % от заказа можно оплатить бонусами

    @Column(name = "registration_bonus")
    private Integer registrationBonus = 100; // бонусы за регистрацию

    @Column(name = "birthday_bonus")
    private Integer birthdayBonus = 200; // бонусы в день рождения

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount = BigDecimal.ZERO; // мин. сумма заказа для списания

    @Column(name = "bonus_expiry_days")
    private Integer bonusExpiryDays = 365; // срок годности бонусов в днях

    @Column(name = "enabled")
    private boolean enabled = true; // включена ли бонусная система
}
