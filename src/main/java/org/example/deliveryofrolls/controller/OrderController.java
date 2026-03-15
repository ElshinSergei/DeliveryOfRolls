package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.OrderDTO;
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

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    // СТРАНИЦА ОФОРМЛЕНИЯ ЗАКАЗА
    @GetMapping("/checkout")
    public String checkout(HttpSession session,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }
        // Создаем DTO и предзаполняем, если пользователь авторизован
        OrderDTO orderDTO = new OrderDTO();
        if (userDetails != null) {
            User user = userService.getCurrentUser(userDetails);
            orderDTO.setCustomerName(user.getFirstName() + " " + user.getLastName());
            orderDTO.setCustomerPhone(user.getPhone());
        }

        model.addAttribute("orderDTO", orderDTO);
        model.addAttribute("pageTitle", "Оформление заказа");
        model.addAttribute("pageCss", "cart.css");
        return "order/checkout";
    }

    // СОЗДАНИЕ ЗАКАЗА
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute OrderDTO orderDTO,
                         HttpSession session,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "order/checkout";
        }

        try {
            Order order = orderService.createOrder(orderDTO, session, userDetails);
            redirectAttributes.addFlashAttribute("success", "Заказ успешно оформлен!");
            return "redirect:/order/confirmation/" + order.getId();
        } catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    // СТРАНИЦА ПОДТВЕРЖДЕНИЯ
    @GetMapping("/confirmation/{orderId}")
    public String confirmation(@PathVariable Long orderId,
                               Model model) {

        try{
            Order order = orderService.getOrder(orderId);

            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "Заказ оформлен");
            model.addAttribute("pageCss", "confirmation.css");

            return "order/confirmation";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
    }
}
