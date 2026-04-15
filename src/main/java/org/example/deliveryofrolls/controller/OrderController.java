package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.OrderDTO;
import org.example.deliveryofrolls.dto.UserAddressDTO;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.service.*;
import org.hibernate.Hibernate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final UserAddressService addressService;
    private final BonusService bonusService;
    private final BonusSettingsService bonusSettingsService;

    // СТРАНИЦА ОФОРМЛЕНИЯ ЗАКАЗА
    @GetMapping("/checkout")
    public String checkout(HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        // ⭐ Конвертируем String в Enum
        Order.DeliveryType deliveryType = convertToDeliveryType(cart.getDeliveryType());

        // Проверяем, выбран ли способ получения в корзине
        if (deliveryType == null) {
            log.warn("Способ получения не выбран в корзине, перенаправляем на выбор адреса");
            return "redirect:/cart";
        }

        // Добавляем DTO в модель
        if (!model.containsAttribute("orderDTO")) {
            OrderDTO orderDTO = new OrderDTO();

            // ⭐ Устанавливаем способ получения из корзины
            orderDTO.setDeliveryType(deliveryType);

            // ⭐ Устанавливаем адрес из корзины
            if (deliveryType == Order.DeliveryType.DELIVERY) {
                orderDTO.setDeliveryAddress(cart.getDeliveryAddress());
            } else if (deliveryType == Order.DeliveryType.PICKUP) {
                orderDTO.setDeliveryAddress(cart.getDeliveryAddress());
            }

            if (userDetails != null) {
                User user = userService.getCurrentUser(userDetails);
                orderDTO.setCustomerName(user.getFirstName() + " " + user.getLastName());
                orderDTO.setCustomerPhone(user.getPhone());

                List<UserAddressDTO> addresses = addressService.getUserAddresses(user);
                model.addAttribute("savedAddresses", addresses);

                BonusAccount bonusAccount = bonusService.getOrCreateAccount(user);
                int maxSpendable = bonusService.getMaxSpendableForCart(cart.getTotalPrice(), user);
                BonusSettings bonusSettings = bonusSettingsService.getSettings();

                model.addAttribute("bonusSettings", bonusSettings);
                model.addAttribute("bonusBalance", bonusAccount.getBalance());
                model.addAttribute("maxSpendable", maxSpendable);
                model.addAttribute("user", user);
            }

            String savedPromoCode = (String) session.getAttribute("appliedPromoCode");
            if (savedPromoCode != null) {
                orderDTO.setAppliedPromoCode(savedPromoCode);
                log.info("Восстановлен промокод из сессии: {}", savedPromoCode);
            }

            model.addAttribute("orderDTO", orderDTO);
        }

        model.addAttribute("cart", cart);
        model.addAttribute("pageTitle", "Оформление заказа");
        model.addAttribute("pageCss", "cart.css");

        return "order/checkout";
    }

    // СОЗДАНИЕ ЗАКАЗА
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute OrderDTO orderDTO,
                         BindingResult bindingResult,
                         HttpSession session,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        // ⭐ Конвертируем String в Enum
        Order.DeliveryType deliveryType = convertToDeliveryType(cart.getDeliveryType());

        // Проверяем, что способ получения выбран в корзине
        if (deliveryType == null) {
            log.error("Способ получения не выбран в корзине");
            bindingResult.rejectValue("deliveryType", "error.orderDTO", "Способ получения не выбран");
            populateCheckoutModel(model, session, userDetails, orderDTO);
            return "order/checkout";
        }

        // ⭐ Устанавливаем способ получения из корзины
        orderDTO.setDeliveryType(deliveryType);

        // ⭐ Устанавливаем адрес из корзины
        if (deliveryType == Order.DeliveryType.DELIVERY) {
            if (cart.getDeliveryAddress() == null || cart.getDeliveryAddress().trim().isEmpty()) {
                bindingResult.rejectValue("deliveryAddress", "error.orderDTO", "Адрес доставки не выбран");
                populateCheckoutModel(model, session, userDetails, orderDTO);
                return "order/checkout";
            }
            orderDTO.setDeliveryAddress(cart.getDeliveryAddress());

        } else if (deliveryType == Order.DeliveryType.PICKUP) {
            if (cart.getDeliveryAddress() == null || cart.getDeliveryAddress().trim().isEmpty()) {
                bindingResult.rejectValue("deliveryAddress", "error.orderDTO", "Точка самовывоза не выбрана");
                populateCheckoutModel(model, session, userDetails, orderDTO);
                return "order/checkout";
            }
            orderDTO.setDeliveryAddress(cart.getDeliveryAddress());

            // Для самовывоза очищаем детали адреса
            orderDTO.setDeliveryEntrance(null);
            orderDTO.setDeliveryFloor(null);
            orderDTO.setDeliveryApartment(null);
            orderDTO.setDeliveryIntercom(null);
        }

        // Проверка адреса для доставки
        if (deliveryType == Order.DeliveryType.DELIVERY) {
            String address = orderDTO.getDeliveryAddress();
            if (address == null || address.trim().isEmpty()) {
                bindingResult.rejectValue("deliveryAddress", "error.orderDTO", "Адрес доставки обязателен");
            } else if (address.length() < 5 || address.length() > 200) {
                bindingResult.rejectValue("deliveryAddress", "error.orderDTO", "Адрес должен быть от 5 до 200 символов");
            }
        }

        if (bindingResult.hasErrors()) {
            log.error("Ошибки валидации: {}", bindingResult.getAllErrors());
            populateCheckoutModel(model, session, userDetails, orderDTO);
            return "order/checkout";
        }

        try {
            Order order = orderService.createOrder(orderDTO, session, userDetails);

            session.removeAttribute("appliedPromoCode");
            session.removeAttribute("promoDiscountAmount");
            session.removeAttribute("promoFinalAmount");

            cartService.clearCart(cart.getId(), session);

            return "redirect:/order/confirmation/" + order.getId();
        } catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            populateCheckoutModel(model, session, userDetails, orderDTO);
            return "order/checkout";
        }
    }

    // СТРАНИЦА ПОДТВЕРЖДЕНИЯ
    @GetMapping("/confirmation/{orderId}")
    public String confirmation(@PathVariable Long orderId,
                               Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Order order = orderService.getOrderWithItems(orderId);

            if (order == null) {
                log.warn("Заказ с id {} не найден", orderId);
                return "redirect:/";
            }

            if (order.getUser() != null && userDetails != null) {
                User currentUser = userService.getCurrentUser(userDetails);
                if (!order.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Пользователь {} пытается просмотреть чужой заказ {}",
                            userDetails.getUsername(), orderId);
                    return "redirect:/";
                }
            }

            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "Заказ оформлен");
            model.addAttribute("pageCss", "confirmation.css");

            return "order/confirmation";

        } catch (IllegalArgumentException e) {
            log.error("Ошибка при получении заказа: {}", e.getMessage());
            return "redirect:/";
        }
    }

    // ⭐ ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ КОНВЕРТАЦИИ
    private Order.DeliveryType convertToDeliveryType(String deliveryTypeStr) {
        if (deliveryTypeStr == null || deliveryTypeStr.isEmpty()) {
            return null;
        }

        try {
            return Order.DeliveryType.valueOf(deliveryTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Неизвестный тип доставки: {}", deliveryTypeStr);
            return null;
        }
    }

    private void populateCheckoutModel(Model model, HttpSession session,
                                       UserDetails userDetails, OrderDTO orderDTO) {
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        model.addAttribute("cart", cart);

        if (userDetails != null) {
            User user = userService.getCurrentUser(userDetails);
            BonusAccount bonusAccount = bonusService.getOrCreateAccount(user);
            int maxSpendable = bonusService.getMaxSpendableForCart(cart.getTotalPrice(), user);
            BonusSettings bonusSettings = bonusSettingsService.getSettings();

            model.addAttribute("bonusSettings", bonusSettings);
            model.addAttribute("bonusBalance", bonusAccount.getBalance());
            model.addAttribute("maxSpendable", maxSpendable);
            model.addAttribute("user", user);

            List<UserAddressDTO> addresses = addressService.getUserAddresses(user);
            model.addAttribute("savedAddresses", addresses);
        }

        String savedPromoCode = (String) session.getAttribute("appliedPromoCode");
        if (savedPromoCode != null) {
            orderDTO.setAppliedPromoCode(savedPromoCode);
        }

        model.addAttribute("orderDTO", orderDTO);
        model.addAttribute("pageTitle", "Оформление заказа");
        model.addAttribute("pageCss", "cart.css");
    }
}