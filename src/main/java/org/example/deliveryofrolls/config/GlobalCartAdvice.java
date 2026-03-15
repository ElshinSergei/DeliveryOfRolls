package org.example.deliveryofrolls.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalCartAdvice {

    private final CartService cartService;

    @ModelAttribute("cart")
    public Cart addCartToModel(HttpSession session,
                               @AuthenticationPrincipal UserDetails userDetails) {

        // Сначала проверяем, есть ли корзина в сессии
        Cart sessionCart = (Cart) session.getAttribute("cart");

        // Если в сессии есть корзина и она не устарела
        if (sessionCart != null && isCartValid(sessionCart)) {
            return sessionCart;
        }

        // Если нет - загружаем из БД
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        session.setAttribute("cart", cart);

        return cart;
    }

    private boolean isCartValid(Cart cart) {
        if (cart == null) return false;
        // Проверяем, что корзина не старше 5 минут
        return cart.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(5));
    }

    @ModelAttribute("contextPath")
    public String getContextPath(HttpServletRequest request) {
        return request.getContextPath();
    }
}
