package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Promotion;
import org.example.deliveryofrolls.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public String listPromotions(Model model) {
        model.addAttribute("promotions", promotionService.getAllPromotions());
        model.addAttribute("pageTitle", "Управление акциями");
        model.addAttribute("pageCss", "admin-promotions.css");
        return "admin/promotions/list";
    }

    @GetMapping("/new")
    public String newPromotion(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("pageTitle", "Добавление акции");
        model.addAttribute("pageCss", "admin-promotions.css");
        return "admin/promotions/form";
    }

    @PostMapping("/save")
    public String savePromotion(@ModelAttribute Promotion promotion,
                                @RequestParam("imageFile") MultipartFile file,
                                RedirectAttributes redirectAttributes) {

        try {
            promotionService.savePromotionWithImage(promotion, file);

            String message = promotion.getId() == null ?
                    "Акция успешно добавлена" :
                    "Акция успешно обновлена";
            redirectAttributes.addFlashAttribute("success", message);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            String redirectUrl = promotion.getId() == null ?
                    "redirect:/admin/promotions/new" :
                    "redirect:/admin/promotions/" + promotion.getId() + "/edit";
            return redirectUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());

            String redirectUrl = promotion.getId() == null ?
                    "redirect:/admin/promotions/new" :
                    "redirect:/admin/promotions/" + promotion.getId() + "/edit";
            return redirectUrl;
        }

        return "redirect:/admin/promotions";
    }

    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        promotionService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Акция успешно удалена");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/{id}/edit")
    public String editPromotion(@PathVariable Long id, Model model) {
        Promotion promotion = promotionService.getById(id);
        model.addAttribute("promotion", promotion);
        model.addAttribute("pageTitle", "Редактирование акции");
        model.addAttribute("pageCss", "admin-promotions.css");
        return "admin/promotions/form";
    }
}
