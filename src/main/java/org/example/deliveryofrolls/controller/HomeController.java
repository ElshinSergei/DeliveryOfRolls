package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.CategoryService;
import org.example.deliveryofrolls.service.DishService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class HomeController {

    private final DishService dishService;
    private final CategoryService categoryService;
    private final CartService cartService;

    @GetMapping("/")
    public String home(HttpSession session,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {

        try {
            List<Category> categories = categoryService.getAllCategories();
            List<Dish> allDishes = dishService.getAllAvailableDishes();
            Cart cart = cartService.getOrCreateCart(session, userDetails);

            model.addAttribute("cart", cart);
            model.addAttribute("categories", categories);
            model.addAttribute("pageTitle", "Доставка суши и роллов | Главная");
            model.addAttribute("pageCss", "home.css");

            return "home";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Временные технические трудности");
            return "error";
        }
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "О нас");
        return "about";
    }

    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("title", "Контакты");
        model.addAttribute("contacts", java.util.Map.of(
                "phone", "+7 (999) 123-45-67",
                "email", "order@sushi-delivery.ru",
                "address", "ул. Примерная, д. 10, Москва",
                "workHours", "Ежедневно с 10:00 до 23:00"
        ));
        return "contacts";
    }

    @GetMapping("/delivery")
    public String delivery(Model model) {
        model.addAttribute("title", "Доставка и оплата");
        return "delivery";
    }

    @GetMapping("/promotions")
    public String promotions(Model model) {
        model.addAttribute("title", "Акции и скидки");
        return "promotions";
    }
}
