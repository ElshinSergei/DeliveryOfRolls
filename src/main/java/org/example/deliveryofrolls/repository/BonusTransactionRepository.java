package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.BonusTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {

    // Найти все транзакции по ID счета с сортировкой по дате (новые сверху)
    List<BonusTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    // Найти транзакции по типу
    List<BonusTransaction> findByAccountIdAndType(Long accountId, BonusTransaction.TransactionType type);

    // Сумма всех начислений
    @Query("SELECT SUM(t.amount) FROM BonusTransaction t WHERE t.account.id = :accountId AND t.amount > 0")
    Integer getTotalEarned(@Param("accountId") Long accountId);

    // Сумма всех списаний
    @Query("SELECT SUM(t.amount) FROM BonusTransaction t WHERE t.account.id = :accountId AND t.amount < 0")
    Integer getTotalSpent(@Param("accountId") Long accountId);
}
