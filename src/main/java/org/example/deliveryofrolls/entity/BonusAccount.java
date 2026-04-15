package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "bonus_accounts")
@Data
public class BonusAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer balance = 0; // текущий баланс бонусов

    @Column(name = "total_earned")
    private Integer totalEarned = 0; // всего начислено за все время

    @Column(name = "total_spent")
    private Integer totalSpent = 0; // всего потрачено за все время

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
