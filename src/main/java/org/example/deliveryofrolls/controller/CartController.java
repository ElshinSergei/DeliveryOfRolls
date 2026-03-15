package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.CartItem;
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

import java.util.HashMap;
import java.util.Map;

@Slf4j
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

        return "cart/cart";
    }

    @PostMapping("/add/{dishId}")
    @ResponseBody
    public ResponseEntity<?> addToCart(
            @PathVariable Long dishId,
            @RequestParam(defaultValue = "1") @Min(1) @Max(99) int quantity,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails) {

            log.info("Добавление товара {} в корзину, количество: {}", dishId, quantity);
        try {
            cartService.addToCart(session, userDetails, dishId, quantity);
            Cart cart = cartService.getOrCreateCart(session, userDetails);
            log.info("Товар добавлен. Всего товаров в корзине: {}", cart.getTotalItems());

            // Формируем JSON ответ
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Товар добавлен в корзину");
            response.put("cartCount", cart.getTotalItems());
            response.put("cartTotal", cart.getTotalPrice());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при добавлении товара {}: {}", dishId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/decrease-dish/{dishId}")
    @ResponseBody
    public ResponseEntity<?> decreaseFromCart(
            @PathVariable Long dishId,  // ← получаем ID блюда
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            cartService.decreaseFromCart(session, userDetails, dishId);
            Cart cart = cartService.getOrCreateCart(session, userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("cartCount", cart.getTotalItems());  // ← обновленный счетчик

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        if (userDetails != null) {
            User user = userService.getCurrentUser(userDetails);
            log.info("Очистка корзины для пользователя: {}, cartId: {}",
                    userDetails.getUsername(), user.getCart().getId());
            cartService.clearCart(user.getCart().getId(), session);
        } else {
            String sessionId = session.getId();
            log.info("Очистка корзины для сессии: {}", sessionId);
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
        cartService.removeItemFromCart(itemId, session);
        return "redirect:/cart";
    }

    // Увеличить на 1
    @PostMapping("/increase/{itemId}")
    public String increaseQuantity(@PathVariable Long itemId,
                                   HttpSession session,
                                   @AuthenticationPrincipal User user) {
        cartService.increaseQuantity(itemId, 1, session);
        return "redirect:/cart";
    }

    // Уменьшить на 1
    @PostMapping("/decrease/{itemId}")
    public String decreaseQuantity(@PathVariable Long itemId,
                                   HttpSession session,
                                   @AuthenticationPrincipal User user) {
        cartService.decreaseQuantity(itemId, 1, session);
        return "redirect:/cart";
    }

    // МЕТОД ДЛЯ ПОЛУЧЕНИЯ СОСТОЯНИЯ
    @GetMapping("/state")
    @ResponseBody
    public ResponseEntity<?> getCartState(HttpSession session,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Cart cart = cartService.getOrCreateCart(session, userDetails);

            Map<Long, Integer> itemCounts = new HashMap<>();
            for (CartItem item : cart.getItems()) {
                itemCounts.put(item.getDish().getId(), item.getQuantity());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalCount", cart.getTotalItems());
            response.put("items", itemCounts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}
