package org.example.deliveryofrolls.config;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalCartAdvice {

    private final CartService cartService;

    @ModelAttribute("cart")
    public Cart addCartToModel(HttpSession session,
                               @AuthenticationPrincipal UserDetails userDetails) {
        return cartService.getOrCreateCart(session, userDetails);
    }

}
