package org.example.deliveryofrolls.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.PromoCode;
import org.example.deliveryofrolls.service.PromoCodeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/marketing/promocodes")
@RequiredArgsConstructor
@Slf4j
public class AdminPromoCodeController {

    private final PromoCodeService promoCodeService;

    /**
     * Список всех промокодов
     */
    @GetMapping
    public String listPromoCodes(Model model) {
        List<PromoCode> promoCodes = promoCodeService.findAll();

        model.addAttribute("promoCodes", promoCodes);
        model.addAttribute("pageTitle", "Управление промокодами");
        model.addAttribute("totalCount", promoCodes.size());
        model.addAttribute("pageCss", "admin-promocodes.css");
        return "admin/marketing/promocodes/list";
    }

    /**
     * Форма создания нового промокода
     */
    @GetMapping("/new")
    public String newPromoCode(Model model) {
        PromoCode promoCode = new PromoCode();
        // Устанавливаем значения по умолчанию для новых промокодов
        if (promoCode.getValidFrom() == null) {
            promoCode.setValidFrom(LocalDateTime.now());
        }
        if (promoCode.getValidUntil() == null) {
            promoCode.setValidUntil(LocalDateTime.now().plusDays(30));
        }
        model.addAttribute("promoCode", promoCode);
        model.addAttribute("discountTypes", PromoCode.DiscountType.values());
        model.addAttribute("pageTitle", "Добавление промокода");
        model.addAttribute("pageCss", "admin-promocodes.css");
        return "admin/marketing/promocodes/form";
    }

    /**
     * Форма редактирования промокода
     */
    @GetMapping("/{id}/edit")
    public String editPromoCode(@PathVariable Long id, Model model) {
        PromoCode promoCode = promoCodeService.findById(id);

        // Добавляем форматированные строки для datetime-local
        model.addAttribute("promoCode", promoCode);
        model.addAttribute("validFromFormatted", formatForDatetimeLocal(promoCode.getValidFrom()));
        model.addAttribute("validUntilFormatted", formatForDatetimeLocal(promoCode.getValidUntil()));
        model.addAttribute("discountTypes", PromoCode.DiscountType.values());
        model.addAttribute("pageTitle", "Редактирование промокода");
        model.addAttribute("pageCss", "admin-promocodes.css");
        return "admin/marketing/promocodes/form";
    }

    /**
     * Сохранение промокода
     */
    @PostMapping("/save")
    public String savePromoCode(@Valid @ModelAttribute PromoCode promoCode,
                                @RequestParam(value = "validFrom", required = false) String validFromStr,
                                @RequestParam(value = "validUntil", required = false) String validUntilStr,
                                @RequestParam(value = "daysOfWeek", required = false) String daysOfWeek,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {

        // Парсим даты из строк
        if (validFromStr != null && !validFromStr.isEmpty()) {
            promoCode.setValidFrom(LocalDateTime.parse(validFromStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (validUntilStr != null && !validUntilStr.isEmpty()) {
            promoCode.setValidUntil(LocalDateTime.parse(validUntilStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (promoCode.getValidFrom() != null && promoCode.getValidUntil() != null) {
            if (promoCode.getValidFrom().isAfter(promoCode.getValidUntil())) {
                bindingResult.rejectValue("validFrom", "error.promoCode",
                        "Дата начала не может быть позже даты окончания");
            }
        }
        // Нормализуем код
        promoCode.setCode(promoCode.getCode().toUpperCase().trim());

        // Валидация уникальности кода
        if (promoCodeService.existsByCodeAndIdNot(promoCode.getCode(), promoCode.getId())) {
            bindingResult.rejectValue("code", "error.promoCode",
                    "Промокод с таким кодом уже существует");
        }

        // ========== ВАЛИДАЦИЯ USAGE_TYPE ==========
        if (promoCode.getUsageType() == PromoCode.UsageType.SINGLE_PER_USER) {
            // Для SINGLE_PER_USER лимит должен быть 1
            promoCode.setUsageLimit(1);
        }

        if (promoCode.getUsageLimit() == null || promoCode.getUsageLimit() < 1) {
            bindingResult.rejectValue("usageLimit", "error.promoCode",
                    "Лимит использований должен быть не менее 1");
        }
        // ==========================================

        if (bindingResult.hasErrors()) {
            return "admin/marketing/promocodes/form";
        }

        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            promoCode.setDaysOfWeek(daysOfWeek);
        }

        try {
            promoCodeService.save(promoCode);

            String message = promoCode.getId() == null ?
                    "Промокод успешно создан" :
                    "Промокод успешно обновлен";
            redirectAttributes.addFlashAttribute("success", message);

        } catch (Exception e) {
            log.error("Ошибка при сохранении промокода", e);
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при сохранении: " + e.getMessage());

            String redirectUrl = promoCode.getId() == null ?
                    "redirect:/admin/marketing/promocodes/new" :
                    "redirect:/admin/marketing/promocodes/" + promoCode.getId() + "/edit";
            return redirectUrl;
        }

        return "redirect:/admin/marketing/promocodes";
    }

    /**
     * Удаление промокода
     */
    @PostMapping("/{id}/delete")
    public String deletePromoCode(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            promoCodeService.delete(id);
            redirectAttributes.addFlashAttribute("success",
                    "Промокод успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при удалении: " + e.getMessage());
        }
        return "redirect:/admin/marketing/promocodes";
    }

    /**
     * Просмотр статистики по промокоду
     */
    @GetMapping("/{id}/stats")
    public String promoCodeStats(@PathVariable Long id, Model model) {
        PromoCode promoCode = promoCodeService.findById(id);
        model.addAttribute("promoCode", promoCode);
        model.addAttribute("pageTitle", "Статистика промокода");
        return "admin/marketing/promocodes/stats";
    }

    /**
     * Быстрое копирование промокода
     */
    @PostMapping("/{id}/duplicate")
    public String duplicatePromoCode(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            PromoCode newPromoCode = promoCodeService.duplicate(id);
            redirectAttributes.addFlashAttribute("success",
                    "Промокод скопирован. Отредактируйте копию.");
            return "redirect:/admin/marketing/promocodes/" + newPromoCode.getId() + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при копировании: " + e.getMessage());
            return "redirect:/admin/marketing/promocodes";
        }
    }

    /**
     * Активация/деактивация промокода
     */
    @PostMapping("/{id}/toggle")
    public String togglePromoCode(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            boolean newStatus = promoCodeService.toggleActive(id);
            String statusText = newStatus ? "активирован" : "деактивирован";
            redirectAttributes.addFlashAttribute("success",
                    "Промокод успешно " + statusText);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Ошибка при изменении статуса: " + e.getMessage());
        }
        return "redirect:/admin/marketing/promocodes";
    }

    /**
     * Страница отчета по промокодам
     */
    @GetMapping("/report")
    public String promoCodeReport(Model model) {
        // Статистика за последние 30 дней
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        Map<String, Object> stats = promoCodeService.getStatistics();

        model.addAttribute("stats", stats);
        model.addAttribute("totalPromoCodes", stats.get("total"));
        model.addAttribute("activePromoCodes", stats.get("active"));
        model.addAttribute("expiredPromoCodes", stats.get("expired"));
        model.addAttribute("totalUsage", stats.get("totalUsage"));
        model.addAttribute("popularPromoCodes", stats.get("popular"));
        model.addAttribute("recentPromoCodes", stats.get("recent"));
        model.addAttribute("pageTitle", "Отчет по промокодам");
        model.addAttribute("pageCss", "admin-promocodes.css");

        return "admin/marketing/promocodes/report";
    }

    // Вспомогательный метод
    private String formatForDatetimeLocal(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}
