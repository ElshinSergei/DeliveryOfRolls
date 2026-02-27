package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // СПИСОК ПОЛЬЗОВАТЕЛЕЙ
    @GetMapping()
    public String listUsers(Model model) {

        List<User> users = userRepository.findAllByOrderByRegisteredAtDesc();

        model.addAttribute("pageTitle", "Управление пользователями");
        model.addAttribute("users", users);

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
        // Получить пользователя по email из Principal
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Нельзя заблокировать самого себя!");
            return "redirect:/admin/users";
        }

        try {
            User user = userService.getUserById(id);
            user.setEnabled(!user.isEnabled());
            String status = user.isEnabled() ? "разблокирован" : "заблокирован";
            redirectAttributes.addFlashAttribute("success",
                    "✅ Пользователь " + user.getEmail() + " " + status);
            userRepository.save(user);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
        }
}
