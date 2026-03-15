package org.example.deliveryofrolls.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.ForgotPasswordDTO;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.example.deliveryofrolls.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Страница запроса email
    @GetMapping("/forgot")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
        model.addAttribute("pageTitle", "Восстановление пароля");
        return "auth/forgot-password";
    }

    // Обработка email и сброс пароля
    @PostMapping("/forgot")
    public String forgotPassword(@Valid ForgotPasswordDTO dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        // Ищем пользователя
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute("success",
                    "Если email зарегистрирован, вы получите инструкции по сбросу пароля");
            return "redirect:/login";
        }

        // Генерируем временный пароль
        String tempPassword = generateTempPassword();

        // Устанавливаем временный пароль
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        try {
            // Отправляем на почту
            emailService.sendPasswordResetEmail(user.getEmail(), tempPassword);

            redirectAttributes.addFlashAttribute("success",
                    "Временный пароль отправлен на ваш email");

        } catch (Exception e) {
            log.error("Ошибка отправки email: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при отправке письма. Попробуйте позже.");
        }
        return "redirect:/login";
    }

    private String generateTempPassword() {
        // генерация временного пароля
        return UUID.randomUUID().toString().substring(0, 8) + "A1!";
    }
}
