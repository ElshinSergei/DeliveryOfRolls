package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Найти все позиции заказа
    List<OrderItem> findByOrder(Order order);

    // Найти все позиции по ID заказа
    List<OrderItem> findByOrderId(Long orderId);

}
