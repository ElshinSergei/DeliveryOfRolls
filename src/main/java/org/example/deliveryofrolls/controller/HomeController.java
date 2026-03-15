package org.example.deliveryofrolls.controller;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.service.CategoryService;
import org.example.deliveryofrolls.service.DishService;
import org.example.deliveryofrolls.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DishService dishService;
    private final CategoryService categoryService;
    private final PromotionService promotionService;


    @GetMapping({"/", "/home"})
    public String home(Model model) {

        try {
            List<Category> categories = categoryService.getAvailableCategories();
            List<Dish> dishes = dishService.getAllAvailableDishes();

            model.addAttribute("promotions", promotionService.getActivePromotions());
            model.addAttribute("categories", categories);
            model.addAttribute("dishes", dishes);
            model.addAttribute("pageTitle", "Доставка суши и роллов | Главная");
            model.addAttribute("pageCss", "home.css");

            return "home";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Временные технические трудности");
            return "error";
        }
    }

    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("pageTitle", "Контакты");
        model.addAttribute("pageCss", "contacts.css");
        return "contacts";
    }

    @GetMapping("/delivery")
    public String delivery(Model model) {
        model.addAttribute("pageTitle", "Доставка и оплата");
        model.addAttribute("pageCss", "delivery.css");
        return "delivery";
    }

    @GetMapping("/promotions")
    public String promotions(Model model) {
        model.addAttribute("activePromotions", promotionService.getActivePromotions());
        model.addAttribute("pageTitle", "Акции и скидки");
        model.addAttribute("pageCss", "promotions.css");
        return "promotions";
    }
}
