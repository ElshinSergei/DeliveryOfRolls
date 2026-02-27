package org.example.deliveryofrolls.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.RegisterDTO;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageCss", "login.css");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        model.addAttribute("pageCss", "register.css");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterDTO registerDto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(registerDto);
            model.addAttribute("pageCss", "register.css");
            redirectAttributes.addFlashAttribute("success",
                    "✅ Регистрация успешна! Войдите в систему.");
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.user", e.getMessage());
            model.addAttribute("pageCss", "register.css");
            return "auth/register";
        }

    }
}
