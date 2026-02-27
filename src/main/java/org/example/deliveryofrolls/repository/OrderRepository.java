package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // Загружаем заказ вместе с товарами и блюдами
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.dish " +
            "WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    // Найти все заказы пользователя по дате
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    // Найти кол-во заказов после указанной даты
    Long countByCreatedAtAfter(LocalDateTime date);

    // Кол-во заказов с определенным статусом
    Long countByStatus(Order.OrderStatus status);

    // Сумма всех заказов за все время
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o")
    BigDecimal sumTotalPrice();

    // Выручка после указанной даты
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.createdAt > :date")
    BigDecimal sumTotalPriceByCreatedAtAfter(@Param("date") LocalDateTime date);

    // ТОП-5 популярных блюд
    @Query("SELECT d.name as name, COUNT(oi) as count " +
            "FROM OrderItem oi " +
            "JOIN oi.dish d " +
            "GROUP BY d.name " +
            "ORDER BY count DESC " +
            "LIMIT 5")
    List<Map<String, Object>> findTopPopularDishes();

    // Заказы между датами
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Заказы после даты
    List<Order> findByCreatedAtGreaterThanEqual(LocalDateTime start);

    // Заказы до даты
    List<Order> findByCreatedAtLessThan(LocalDateTime end);

}
