package org.example.deliveryofrolls.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.example.deliveryofrolls.service.CategoryService;
import org.example.deliveryofrolls.service.DishService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/dishes")
@RequiredArgsConstructor
@Slf4j
public class AdminDishController {

    private final DishRepository dishRepository;
    private final DishService dishService;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    // СПИСОК ВСЕХ БЛЮД
    @GetMapping
    public String listDishes(@RequestParam(required = false) String view,  // view=archive для архива
                             @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                             Pageable pageable,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) Long category,
                             @RequestParam(required = false) Boolean available,
                             HttpServletRequest request,
                             Model model) {

        Page<Dish> dishes;

        if ("archive".equals(view)) {
            // АРХИВ - только удаленные блюда
            dishes = dishService.findArchivedDishes(search, category, pageable);
            model.addAttribute("isArchive", true);
        } else {
            // МЕНЮ - только активные (не удаленные)
            dishes = dishService.findActiveDishes(search, category, available, pageable);
            model.addAttribute("isArchive", false);
        }

        String currentSortField = "";
        String currentSortDir = "asc";
        String reverseSortDir = "desc";

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            currentSortField = order.getProperty();
            currentSortDir = order.getDirection().name().toLowerCase();
            reverseSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        } else {
            currentSortField = "createdAt";
        }

        model.addAttribute("currentSortField", currentSortField);
        model.addAttribute("currentSortDir", currentSortDir);
        model.addAttribute("reverseSortDir", reverseSortDir);
        model.addAttribute("dishes", dishes);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("available", available);
        model.addAttribute("pageTitle", "Управление меню");
        model.addAttribute("baseUrl", "/admin/dishes");;
        return "admin/dishes/list";
    }

    // ФОРМА ДОБАВЛЕНИЯ
    @GetMapping("/new")
    public String newDish(Model model) {
        model.addAttribute("dish", new Dish());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Добавление блюда");
        return "admin/dishes/form";
    }

    // ФОРМА РЕДАКТИРОВАНИЯ
    @GetMapping("/{id}/edit")
    public String editDish(@PathVariable Long id, Model model) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено: " + id));

        model.addAttribute("dish", dish);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Редактирование блюда");
        return "admin/dishes/form";
    }

    // СОХРАНЕНИЕ (ДОБАВЛЕНИЕ ИЛИ ОБНОВЛЕНИЕ)
    @PostMapping("/save")
    public String saveDish(@ModelAttribute Dish dish,
                           @RequestParam("imageFile") MultipartFile file,
                           @RequestParam(value = "ingredientsString", required = false) String ingredientsString,
                           RedirectAttributes redirectAttributes) {

        try {
            dishService.saveDishWithImage(dish, file, ingredientsString);

            String message = dish.getId() == null ?
                    "✅ Блюдо успешно добавлено" :
                    "✅ Блюдо \"" + dish.getName() + "\" успешно обновлено";

            redirectAttributes.addFlashAttribute("success", message);
            return "redirect:/admin/dishes";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при сохранении блюда", e);
            redirectAttributes.addFlashAttribute("error", "❌ Ошибка: " + e.getMessage());
        }

        String redirectUrl = dish.getId() == null ?
                "redirect:/admin/dishes/new" :
                "redirect:/admin/dishes/" + dish.getId() + "/edit";
        return redirectUrl;

    }

    // УДАЛЕНИЕ (В АРХИВ)
    @PostMapping("/{id}/delete")
    public String deleteDish(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        Dish dish = dishService.getDishById(id);
        String dishName = dish.getName();
        dishService.deleteDish(id);
        redirectAttributes.addFlashAttribute("success",
                "✅ Блюдо \"" + dishName + "\" перемещено в архив");


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

    // ВОССТАНОВЛЕНИЕ ИЗ АРХИВА
    @PostMapping("/{id}/restore")
    public String restoreDish(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            Dish dish = dishService.getDishByIdIncludingDeleted(id);
            dishService.restoreDish(id);
            redirectAttributes.addFlashAttribute("success",
                    "✅ Блюдо \"" + dish.getName() + "\" восстановлено из архива");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/dishes?view=archive";
    }
}
