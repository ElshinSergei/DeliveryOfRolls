package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.OrderDTO;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;

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
        if (userDetails instanceof User user) {
            orderDTO.setCustomerName(user.getFirstName() + " " + user.getLastName());
            orderDTO.setCustomerPhone(user.getPhone());
        }

        model.addAttribute("cart", cart);
        model.addAttribute("orderDTO", new OrderDTO());
        model.addAttribute("pageTitle", "Оформление заказа");
        model.addAttribute("pageCss", "checkout.css");
        return "order/checkout";
    }

    // СОЗДАНИЕ ЗАКАЗА
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute OrderDTO orderDTO,
                         HttpSession session,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Создаем заказ
        Order order = orderService.createOrder(orderDTO, session, userDetails);

        // Перенаправляем на страницу подтверждения
        return "redirect:/order/confirmation/" + order.getId();
    }

    // СТРАНИЦА ПОДТВЕРЖДЕНИЯ
    @GetMapping("/confirmation/{orderId}")
    public String confirmation(@PathVariable Long orderId,
                               Model model,
                               HttpSession session,
                               @AuthenticationPrincipal UserDetails userDetails) {

        try{
            Order order = orderService.getOrder(orderId);

            Cart cart = cartService.getOrCreateCart(session, userDetails);
            model.addAttribute("cart", cart);
            model.addAttribute("order", order);
            model.addAttribute("pageTitle", "Заказ оформлен");
            model.addAttribute("pageCss", "confirmation.css");

            return "order/confirmation";
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }

    }


}
