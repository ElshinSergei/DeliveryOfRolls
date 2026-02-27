package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Найти корзину по пользователю
    Optional<Cart> findByUser(User user);

    // Найти корзину по ID пользователя
    Optional<Cart> findByUserId(Long userId);

    // Проверить существование корзины у пользователя
    boolean existsByUserId(Long userId);

    // Удалить корзину по пользователю
    void deleteByUser(User user);

    // Удалить корзину по ID пользователя
    void deleteByUserId(Long userId);

    // Поиск корзины по сессии
    Optional<Cart> findBySessionId(String sessionId);
}
