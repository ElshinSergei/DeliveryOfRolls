package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.ChangePasswordDTO;
import org.example.deliveryofrolls.dto.ProfileDTO;
import org.example.deliveryofrolls.entity.BonusAccount;
import org.example.deliveryofrolls.entity.BonusTransaction;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.BonusService;
import org.example.deliveryofrolls.service.OrderService;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final OrderService orderService;
    private final BonusService bonusService;

    // Главная страница профиля
    @GetMapping
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
                          Model model) {

        User user = userService.getCurrentUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Личный кабинет");
        model.addAttribute("pageCss", "profile.css");

        return "profile/index";
    }

    // История заказов пользователя
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails,
                         Model model) {

        User user = userService.getCurrentUser(userDetails);
        List<Order> orders = orderService.getUserOrders(user.getId());
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
                               Model model) {

        try {
            User user = userService.getCurrentUser(userDetails);
            Order order = orderService.getOrderWithItems(orderId);

            // Проверка принадлежности
            if (order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Это не ваш заказ");
            }

            model.addAttribute("order", order);
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "Детальный просмотр заказа");
            model.addAttribute("pageCss", "profile.css");
            return "profile/order-details";

        } catch (Exception e) {
            log.error("Ошибка при просмотре заказа {}: {}", orderId, e.getMessage());
            return "redirect:/profile/orders?error=notfound";
        }
    }

    // Редактирование профиля
    @GetMapping("/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails,
                              Model model) {

        User user = userService.getCurrentUser(userDetails);

        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setFirstName(user.getFirstName());
        profileDTO.setLastName(user.getLastName());
        profileDTO.setPhone(user.getPhone());
        profileDTO.setBirthDate(user.getBirthDate());

        // Добавляем отформатированную дату отдельно
        String birthDateFormatted = user.getBirthDate() != null ?
                user.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";

        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Редактирование профиля");
        model.addAttribute("pageCss", "profile.css");
        model.addAttribute("birthDateFormatted", birthDateFormatted);

        return "profile/edit";
    }

    // Сохранение изменений профиля
    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute ProfileDTO profileDTO,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

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

        try {
            orderService.repeatOrder(orderId, session, userDetails);
            redirectAttributes.addFlashAttribute("success", "Товары добавлены в корзину");
        } catch (Exception e) {
            log.error("Ошибка при повторе заказа {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    // Страница смены пароля
    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        model.addAttribute("pageTitle", "Смена пароля");
        model.addAttribute("pageCss", "profile.css");
        return "profile/change-password";
    }

    // Обработка смены пароля
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordDTO dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        // Проверяем, что новый пароль и подтверждение совпадают
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error", "Пароли не совпадают");
        }

        if (bindingResult.hasErrors()) {
            return "profile/change-password";
        }

        try {
            User user = userService.getCurrentUser(userDetails);
            userService.changePassword(user.getId(), dto.getOldPassword(), dto.getNewPassword());

            redirectAttributes.addFlashAttribute("success", "Пароль успешно изменен");
            return "redirect:/profile";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("oldPassword", "error", e.getMessage());
            return "profile/change-password";
        }
    }

    // Мои бонусы
    @GetMapping("/bonuses")
    public String bonuses(@AuthenticationPrincipal UserDetails userDetails,
                          Model model) {

        User user = userService.getCurrentUser(userDetails);
        BonusAccount account = bonusService.getOrCreateAccount(user);
        List<BonusTransaction> transactions = bonusService.getTransactionHistory(user);

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("pageTitle", "Мои бонусы");
        model.addAttribute("pageCss", "profile.css");

        return "profile/bonuses";
    }

}
