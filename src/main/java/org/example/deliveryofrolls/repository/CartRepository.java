package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Найти корзину по пользователю
    Optional<Cart> findByUser(User user);

    // Поиск корзины по сессии
    Optional<Cart> findBySessionId(String sessionId);

    // Получить корзину с элементами по сессии
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);

}
