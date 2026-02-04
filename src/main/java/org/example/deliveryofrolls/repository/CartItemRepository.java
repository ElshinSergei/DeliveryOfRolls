package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.CartItem;
import org.example.deliveryofrolls.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Найти все элементы корзины
    List<CartItem> findByCart(Cart cart);

    // Найти элемент по корзине и блюду
    Optional<CartItem> findByCartAndDish(Cart cart, Dish dish);

    // Удалить все элементы корзины
    void deleteByCart(Cart cart);

    // Количество элементов в корзине
    Long countByCart(Cart cart);

    // Сумма корзины (через запрос)
    @Query("SELECT SUM(ci.quantity * d.price) " +
            "FROM CartItem ci JOIN ci.dish d " +
            "WHERE ci.cart = ?1")
    BigDecimal calculateCartTotal(Cart cart);
}
