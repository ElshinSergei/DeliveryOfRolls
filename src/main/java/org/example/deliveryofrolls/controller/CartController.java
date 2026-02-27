package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public String viewCart(Model model,
                           HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);
        model.addAttribute("cart", cart);
        model.addAttribute("pageTitle", "Корзина");
        model.addAttribute("pageCss", "cart.css");

        BigDecimal deliveryCost = cart.getTotalPrice();
        model.addAttribute("deliveryCost", deliveryCost);

        return "cart/cart";
    }

    @PostMapping("/add/{dishId}")
    public String addToCart(
            @PathVariable Long dishId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails) {

            cartService.addToCart(session, userDetails, dishId, quantity);

            return "redirect:/menu";
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        if (userDetails != null) {
            User user = userService.getCurrentUser(userDetails);
            cartService.clearCart(user.getCart().getId());
        } else {
            // Для неавторизованного (через сессию)
            String sessionId = session.getId();
            cartService.clearCart(sessionId);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Корзина успешно очищена");
        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeItem(@PathVariable Long itemId,
                             HttpSession session,
                             @AuthenticationPrincipal User user,
                             Model model) {
        cartService.removeItemFromCart(itemId);
        return "redirect:/cart";
    }

    // Увеличить на 1
    @PostMapping("/increase/{itemId}")
    public String increaseQuantity(@PathVariable Long itemId,
                                   HttpSession session,
                                   @AuthenticationPrincipal User user) {
        cartService.increaseQuantity(itemId, 1);
        return "redirect:/cart";
    }

    // Уменьшить на 1
    @PostMapping("/decrease/{itemId}")
    public String decreaseQuantity(@PathVariable Long itemId,
                                   HttpSession session,
                                   @AuthenticationPrincipal User user) {
        cartService.decreaseQuantity(itemId, 1);
        return "redirect:/cart";
    }


}
