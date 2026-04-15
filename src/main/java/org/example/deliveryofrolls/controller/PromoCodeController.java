package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.PromoCodeRequest;
import org.example.deliveryofrolls.dto.PromoCodeResponse;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.PromoCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/promo")
@RequiredArgsConstructor
@Slf4j
public class PromoCodeController {

    private final PromoCodeService promoCodeService;
    private final CartService cartService;

    /**
     * Эндпоинт для применения промокода
     * URL: POST /api/promo/apply
     * Тело запроса: { "code": "WELCOME10" }
     * Ответ: JSON с результатом
     */
    @PostMapping("/apply")
    @ResponseBody
    public ResponseEntity<PromoCodeResponse> applyPromoCode(
            @RequestBody PromoCodeRequest request,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("📩 Получен запрос на применение промокода: {}", request.getCode());

        try {
            // Получаем сумму корзины
            BigDecimal cartTotal = cartService.getOrCreateCart(session, userDetails).getTotalPrice();
            log.info("Сумма корзины: {} ₽", cartTotal);

            // Проверяем промокод
            PromoCodeResponse response = promoCodeService.applyPromoCode(
                    request.getCode(),
                    cartTotal,
                    userDetails
            );

            // Логируем результат
            if (response.isValid()) {
                // Сохраняем промокод в сессию
                session.setAttribute("appliedPromoCode", request.getCode());
                session.setAttribute("promoDiscountAmount", response.getDiscountAmount());
                session.setAttribute("promoFinalAmount", response.getFinalAmount());
                log.info("✅ Промокод применен. Скидка: {} ₽", response.getDiscountAmount());
            } else {
                // Если промокод невалиден, удаляем из сессии
                session.removeAttribute("appliedPromoCode");
                session.removeAttribute("promoDiscountAmount");
                session.removeAttribute("promoFinalAmount");
                log.info("❌ Промокод не применен: {}", response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при обработке промокода: {}", e.getMessage(), e);

            PromoCodeResponse errorResponse = PromoCodeResponse.error(
                    "Ошибка при проверке промокода"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPromo(HttpServletRequest request) {

        try {
            HttpSession session = request.getSession();
            session.removeAttribute("appliedPromoCode");
            session.removeAttribute("promoDiscount");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Промокод отменен");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при отмене промокода");

            return ResponseEntity.badRequest().body(response);
        }
    }
}
