package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.BonusSettings;
import org.example.deliveryofrolls.service.BonusSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/marketing")
@RequiredArgsConstructor
public class AdminBonusController {

    private final BonusSettingsService bonusSettingsService;

    @GetMapping("/bonus-settings")
    public String bonusSettings(Model model) {
        model.addAttribute("settings", bonusSettingsService.getSettings());
        model.addAttribute("pageTitle", "Настройки бонусной системы");
        model.addAttribute("pageCss", "admin-marketing.css");
        return "admin/marketing/bonus-settings";
    }

    @PostMapping("/bonus-settings")
    public String saveBonusSettings(@ModelAttribute BonusSettings settings,
                                    RedirectAttributes redirectAttributes) {
        try {
            bonusSettingsService.updateSettings(settings);
            redirectAttributes.addFlashAttribute("success", "Настройки сохранены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/marketing/bonus-settings";
    }

}
