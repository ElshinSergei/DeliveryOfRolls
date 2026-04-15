package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.service.BonusService;
import org.example.deliveryofrolls.service.PromoCodeService;
import org.example.deliveryofrolls.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/marketing")
@RequiredArgsConstructor
public class AdminMarketingDashboardController {

    private final PromotionService promotionService;
    private final PromoCodeService promoCodeService;
    private final BonusService bonusService;

    @GetMapping
    public String dashboard(Model model) {
        // Статистика по акциям
        model.addAttribute("promotionsCount", promotionService.count());
        model.addAttribute("activePromotionsCount", promotionService.countActive());
        model.addAttribute("recentPromotions", promotionService.findRecent(5));

        // Статистика по промокодам
        model.addAttribute("promoCodesCount", promoCodeService.count());
        model.addAttribute("activePromoCodesCount", promoCodeService.countActive());
        model.addAttribute("expiredPromoCodesCount", promoCodeService.countExpired());
        model.addAttribute("totalUsageCount", promoCodeService.getTotalUsageCount());
        model.addAttribute("popularPromoCodes", promoCodeService.findMostUsed(5));

        model.addAttribute("totalBonusBalance", bonusService.getTotalBonusBalance());
        model.addAttribute("activeBonusUsers", bonusService.getActiveBonusUsersCount());

        model.addAttribute("pageTitle", "Маркетинг");
        model.addAttribute("pageCss", "admin-marketing.css");
        return "admin/marketing/dashboard";
    }
}
