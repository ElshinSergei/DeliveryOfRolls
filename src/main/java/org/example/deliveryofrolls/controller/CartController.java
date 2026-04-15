package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.repository.DishRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;

    @GetMapping
    public String viewCart(Model model,
                           HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        // Загружаем аксессуары
        Category accessoriesCat = categoryRepository.findByName("Аксессуары").orElse(null);
        List<Dish> accessories = new ArrayList<>();
        if (accessoriesCat != null) {
            // Только доступные блюда из категории аксессуаров
            accessories = dishRepository.findByCategoryAndAvailableTrue(accessoriesCat);
        }

        model.addAttribute("cart", cart);
        model.addAttribute("accessories", accessories);
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
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (CartItem item : cart.getItems()) {
                itemCounts.put(item.getDish().getId(), item.getQuantity());
                // Суммируем общую стоимость
                totalAmount = totalAmount.add(item.getTotalPrice());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalCount", cart.getTotalItems());
            response.put("totalAmount", totalAmount);
            response.put("items", itemCounts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Сохранить информацию о доставке (вызывается из maps.js)
     */
    @PostMapping("/delivery-info")
    @ResponseBody
    public ResponseEntity<?> updateDeliveryInfo(@RequestBody Map<String, Object> deliveryInfo,
                                                HttpSession session,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Cart cart = cartService.getOrCreateCart(session, userDetails);
            String type = (String) deliveryInfo.get("type");

            cart.setDeliveryType(type);

            if ("delivery".equals(type)) {
                Map<String, Object> zone = (Map<String, Object>) deliveryInfo.get("zone");
                if (zone != null) {
                    cart.setDeliveryAddress((String) deliveryInfo.get("text"));
                    cart.setDeliveryTime((String) zone.get("deliveryTime"));
                    cart.setMinOrderRequired(((Number) zone.get("minOrder")).intValue());

                    // Сохраняем ID зоны, если есть
                    if (zone.get("id") != null) {
                        cart.setSelectedZoneId(((Number) zone.get("id")).longValue());
                    }
                    cart.setSelectedZoneName((String) zone.get("name"));
                    cart.setPickupPointId(null);
                    cart.setPickupPointName(null);

                    log.info("Сохранена доставка: address={}, zone={}, minOrder={}",
                            cart.getDeliveryAddress(), cart.getSelectedZoneName(), cart.getMinOrderRequired());
                }
            } else if ("pickup".equals(type)) {
                Map<String, Object> point = (Map<String, Object>) deliveryInfo.get("point");
                if (point != null) {
                    cart.setDeliveryAddress((String) point.get("address"));
                    cart.setPickupPointId(((Number) point.get("id")).longValue());
                    cart.setPickupPointName((String) point.get("name"));
                    cart.setDeliveryTime(null);
                    cart.setMinOrderRequired(null);
                    cart.setSelectedZoneId(null);
                    cart.setSelectedZoneName(null);

                    log.info("Сохранен самовывоз: point={}, address={}",
                            cart.getPickupPointName(), cart.getDeliveryAddress());
                }
            }

            cartService.save(cart);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("deliveryType", cart.getDeliveryType());

            if ("delivery".equals(type) && cart.getMinOrderRequired() != null) {
                BigDecimal currentTotal = cart.getTotalPrice();
                boolean isMinOrderMet = currentTotal.compareTo(BigDecimal.valueOf(cart.getMinOrderRequired())) >= 0;
                response.put("minOrderRequired", cart.getMinOrderRequired());
                response.put("currentTotal", currentTotal);
                response.put("isMinOrderMet", isMinOrderMet);
                response.put("needToAdd", isMinOrderMet ? 0 : cart.getMinOrderRequired() - currentTotal.intValue());
                response.put("address", cart.getDeliveryAddress());
                response.put("zoneName", cart.getSelectedZoneName());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка сохранения информации о доставке", e);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Получить информацию о доставке для отображения в корзине и оформлении
     */
    @GetMapping("/delivery-info")
    @ResponseBody
    public ResponseEntity<?> getDeliveryInfo(HttpSession session,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Cart cart = cartService.getOrCreateCart(session, userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("hasDeliveryInfo", cart.getDeliveryType() != null);
            response.put("deliveryType", cart.getDeliveryType());

            if ("DELIVERY".equals(cart.getDeliveryType()) || "delivery".equals(cart.getDeliveryType())) {
                response.put("address", cart.getDeliveryAddress());
                response.put("deliveryTime", cart.getDeliveryTime());
                response.put("minOrderRequired", cart.getMinOrderRequired());
                response.put("zoneName", cart.getSelectedZoneName());
                response.put("currentTotal", cart.getTotalPrice());
                response.put("zoneId", cart.getSelectedZoneId());

                log.info("Информация о доставке: address={}, zoneName={}, minOrder={}",
                        cart.getDeliveryAddress(), cart.getSelectedZoneName(), cart.getMinOrderRequired());

            } else if ("PICKUP".equals(cart.getDeliveryType()) || "pickup".equals(cart.getDeliveryType())) {
                response.put("address", cart.getDeliveryAddress());
                response.put("pointName", cart.getPickupPointName());
                response.put("pointId", cart.getPickupPointId());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка получения информации о доставке", e);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Получить количество товаров в корзине (для счетчика в хедере)
     */
    @GetMapping("/count")
    @ResponseBody
    public int getCartCount(HttpSession session,
                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Cart cart = cartService.getOrCreateCart(session, userDetails);
            return cart.getTotalItems();
        } catch (Exception e) {
            log.error("Ошибка получения количества товаров в корзине", e);
            return 0;
        }
    }
}
