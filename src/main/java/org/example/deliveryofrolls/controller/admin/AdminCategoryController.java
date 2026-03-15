package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Category;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    // СПИСОК КАТЕГОРИЙ
    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Управление категориями");
        return "admin/categories/list";
    }

    // ФОРМА ДОБАВЛЕНИЯ
    @GetMapping("/new")
    public String newCategory(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("pageTitle", "Добавление категории");
        return "admin/categories/form";
    }

    // ФОРМА РЕДАКТИРОВАНИЯ
    @GetMapping("/{id}/edit")
    public String editCategory(@PathVariable Long id,
                              Model model) {
        Category category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        model.addAttribute("pageTitle", "Редактирование категории");

        return "admin/categories/form";
    }

    // СОХРАНЕНИЕ (ДОБАВЛЕНИЕ ИЛИ ОБНОВЛЕНИЕ)
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute Category category,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        categoryService.saveCategory(category);
        redirectAttributes.addFlashAttribute("success",
                "✅ Категория \"" + category.getName() + "\" успешно сохранена");

        return "redirect:/admin/categories";
    }

    // УДАЛЕНИЕ
    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        Category category = categoryService.getCategoryById(id);
        if (!category.getDishes().isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Нельзя удалить категорию, в ней есть блюда");
            return "redirect:/admin/categories";
        }
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("success",
                "✅ Категория \"" + category.getName() + "\" удалена");

        return "redirect:/admin/categories";
    }

    // ПЕРЕКЛЮЧЕНИЕ ДОСТУПНОСТИ
    @PostMapping("/{id}/toggle")
    public String toggleCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        Category category = categoryService.getCategoryById(id);
        categoryService.toggleAvailability(id);
        String status = category.isAvailable() ? "доступна" : "скрыта";
        redirectAttributes.addFlashAttribute("success",
                "✅ Категория \"" + category.getName() + "\" теперь " + status);
        return "redirect:/admin/categories";
    }
}
