package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.ProfileDTO;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.OrderService;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final CartService cartService;

    // Главная страница профиля
    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
                          HttpSession session,
                          Model model) {

        User user = userService.getCurrentUser(userDetails);
        Cart cart = cartService.getOrCreateCart(session, userDetails);

        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Личный кабинет");
        model.addAttribute("pageCss", "profile.css");

        return "profile/index";
    }

    // История заказов пользователя
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails,
                         HttpSession session,
                         Model model) {

        User user = userService.getCurrentUser(userDetails);
        List<Order> orders = orderService.getUserOrders(user);
        Cart cart = cartService.getOrCreateCart(session, userDetails);

        model.addAttribute("cart", cart);
        model.addAttribute("orders", orders);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Мои заказы");
        model.addAttribute("pageCss", "profile.css");

        return "profile/orders";
    }

    // Детальный просмотр заказа
    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable Long orderId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session,
                               Model model) {

        User user = userService.getCurrentUser(userDetails);
        Order order =  orderService.getOrderWithItems(orderId);
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        model.addAttribute("cart", cart);
        model.addAttribute("order", order);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Заказ оформлен");
        model.addAttribute("pageCss", "profile.css");

        return "profile/order-details";
    }

    // Редактирование профиля
    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails,
                              HttpSession session,
                              Model model) {

        User user = userService.getCurrentUser(userDetails);
        Cart cart = cartService.getOrCreateCart(session, userDetails);

        // Заполняем DTO данными пользователя
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setFirstName(user.getFirstName());
        profileDTO.setLastName(user.getLastName());
        profileDTO.setPhone(user.getPhone());

        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Редактирование профиля");
        model.addAttribute("pageCss", "profile.css");

        return "profile/edit";
    }

    // Сохранение изменений профиля
    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute ProfileDTO profileDTO,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

        // Проверка ошибок валидации
        if (bindingResult.hasErrors()) {
            return "profile/edit";
        }

        try {
            User user = userService.getCurrentUser(userDetails);
            userService.updateProfile(user.getId(), profileDTO);
            redirectAttributes.addFlashAttribute("success", "Данные успешно обновлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    // ПОВТОРИТЬ ЗАКАЗ
    @PostMapping("/orders/{orderId}/repeat")
    public String repeatOrder(@PathVariable Long orderId,
                              HttpSession session,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

            orderService.repeatOrder(orderId, session, userDetails);

            return "redirect:/cart";
    }
}
