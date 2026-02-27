package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.CategoryService;
import org.example.deliveryofrolls.service.DishService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final DishService dishService;
    private final CategoryService categoryService;
    private final CartService cartService;

    @GetMapping("")
    public String menu(HttpSession session,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        model.addAttribute("cart", cart);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("dishes", dishService.getAllAvailableDishes());
        model.addAttribute("pageTitle", "Меню");
        model.addAttribute("activeCategoryId", 0); // 0 = "Все блюда"
        model.addAttribute("pageCss", "menu.css");

        return "menu";
    }

    // Меню по категории
    @GetMapping("/category/{categoryId}")
    public String menuByCategory(@PathVariable Long categoryId,
                                 HttpSession session,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        model.addAttribute("cart", cart);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("dishes", dishService.getDishesByCategory(categoryId));
        model.addAttribute("pageTitle", "Меню");
        model.addAttribute("activeCategoryId", categoryId);
        model.addAttribute("pageCss", "menu.css");

        return "menu";
    }
}
