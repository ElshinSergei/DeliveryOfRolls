package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Найти заказы пользователя
    List<Order> findByUser(String name);

    // Найти заказы по ID пользователя
    List<Order> findByUserId(Long userId);

    // Найти заказы по статусу
    List<Order> findByStatus(Order.OrderStatus status);

    // Найти заказы за период
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Найти заказы на доставку
    List<Order> findByDeliveryType(Order.DeliveryType deliveryType);

}
