package org.example.deliveryofrolls.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // СПИСОК ПОЛЬЗОВАТЕЛЕЙ
    @GetMapping()
    public String listUsers(@PageableDefault(size = 20, sort = "registeredAt", direction = Sort.Direction.DESC)
                            Pageable pageable,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Boolean enabled,
                            HttpServletRequest request,
                            Model model) {

        Page<User> users = userService.findUsersByFilters(search, role, enabled, pageable);

        // Получаем параметры сортировки из Pageable
        String currentSortField = "";
        String currentSortDir = "desc";
        String reverseSortDir = "asc";

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            currentSortField = order.getProperty();
            currentSortDir = order.getDirection().name().toLowerCase();
            reverseSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        } else {
            currentSortField = "registeredAt";
        }

        model.addAttribute("currentSortField", currentSortField);
        model.addAttribute("currentSortDir", currentSortDir);
        model.addAttribute("reverseSortDir", reverseSortDir);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", enabled);
        model.addAttribute("searchQuery", search);
        model.addAttribute("pageTitle", "Управление пользователями");
        model.addAttribute("users", users);
        model.addAttribute("baseUrl", "/admin/users");

        return "admin/users/list";
    }

    // ПРОСМОТР ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {

        User user = userService.getUserById(id);

        model.addAttribute("pageTitle", "Просмотр пользователя");
        model.addAttribute("user", user);

        return "admin/users/view";
    }

    // РЕДАКТИРОВАНИЕ ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {

        User user = userService.getUserById(id);

        model.addAttribute("pageTitle", "Редактирование пользователя");
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("user", user);

        return "admin/users/form";
    }

    // СОХРАНЕНИЕ ИЗМЕНЕНИЙ
    @PostMapping("/{id}/save")
    public String saveUser(@PathVariable Long id,
                           @RequestParam User.Role role,
                           @RequestParam(required = false) boolean enabled,
                           RedirectAttributes redirectAttributes) {

        try {
            User user = userService.getUserById(id);
            user.setRole(role);
            user.setEnabled(enabled);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Данные пользователя обновлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Ошибка: " + e.getMessage());
        }

        return "redirect:/admin/users/" + id;
    }

    // БЛОКИРОВКА/РАЗБЛОКИРОВКА
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }
        try {
            // Получаем текущего админа
            String email = principal.getName();
            User currentUser = userService.getCurrentUserByEmail(email);
            // Выполняем блокировку/разблокировку
            userService.toggleUserStatus(id, currentUser);
            // Получаем обновленного пользователя для сообщения
            User user = userService.getUserById(id);
            String status = userService.getUserStatusMessage(user);

            redirectAttributes.addFlashAttribute("success",
                    "✅ Пользователь " + user.getEmail() + " " + status);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Ошибка: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
}
