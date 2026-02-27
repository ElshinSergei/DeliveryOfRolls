package org.example.deliveryofrolls.controller.admin;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.example.deliveryofrolls.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/dishes")
@RequiredArgsConstructor
public class AdminDishController {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final CartService cartService;

    // СПИСОК ВСЕХ БЛЮД
    @GetMapping
    public String listDishes(Model model) {
        List<Dish> dishes = dishRepository.findAllByOrderByName();
        model.addAttribute("dishes", dishes);
        model.addAttribute("pageTitle", "Управление меню");
        return "admin/dishes/list";
    }

    // ФОРМА ДОБАВЛЕНИЯ
    @GetMapping("/new")
    public String newDish(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("dish", new Dish());
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Добавление блюда");
        return "admin/dishes/form";
    }

    // ФОРМА РЕДАКТИРОВАНИЯ
    @GetMapping("/{id}/edit")
    public String editDish(@PathVariable Long id, Model model) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено: " + id));
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("dish", dish);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Редактирование блюда");
        return "admin/dishes/form";
    }

    // СОХРАНЕНИЕ (ДОБАВЛЕНИЕ ИЛИ ОБНОВЛЕНИЕ)
    @PostMapping("/save")
    public String saveDish(@ModelAttribute Dish dish,
                           @RequestParam Long categoryId,
                           RedirectAttributes redirectAttributes) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        dish.setCategory(category);
        dishRepository.save(dish);
        redirectAttributes.addFlashAttribute("success",
                "✅ Блюдо \"" + dish.getName() + "\" успешно сохранено");

        return "redirect:/admin/dishes";
    }

    // УДАЛЕНИЕ
    @PostMapping("/{id}/delete")
    public String deleteDish(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

        String dishName = dish.getName();
        dishRepository.delete(dish);
        redirectAttributes.addFlashAttribute("success",
                "✅ Блюдо \"" + dishName + "\" удалено");


        return "redirect:/admin/dishes";
    }

    // ИЗМЕНЕНИЕ ДОСТУПНОСТИ
    @PostMapping("/{id}/toggle-availability")
    public String toggleAvailability(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {

            Dish dish = dishRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

            dish.setAvailable(!dish.isAvailable());
            dishRepository.save(dish);
            String status = dish.isAvailable() ? "доступно" : "недоступно";
            redirectAttributes.addFlashAttribute("success",
                    "✅ Блюдо \"" + dish.getName() + "\" теперь " + status);


        return "redirect:/admin/dishes";
    }


}
