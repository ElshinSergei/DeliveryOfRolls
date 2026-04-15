package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.BonusAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BonusAccountRepository extends JpaRepository<BonusAccount, Long> {

    Optional<BonusAccount> findByUserId(Long userId);

    // Количество пользователей с положительным балансом
    @Query("SELECT COUNT(a) FROM BonusAccount a WHERE a.balance > 0")
    long countByBalanceGreaterThan();

    // Общий баланс всех бонусов
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM BonusAccount a")
    long getTotalBonusBalance();
}
