package org.example.deliveryofrolls.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.BonusRequest;
import org.example.deliveryofrolls.dto.BonusResponse;
import org.example.deliveryofrolls.entity.BonusAccount;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.BonusService;
import org.example.deliveryofrolls.service.CartService;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/bonus")
@RequiredArgsConstructor
@Slf4j
public class BonusController {

    private final BonusService bonusService;
    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/calculate")
    @ResponseBody
    public ResponseEntity<BonusResponse> calculateBonus(
            @RequestBody BonusRequest request,
            HttpSession session,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(BonusResponse.error("Только для авторизованных"));
        }

        try {
            User user = userService.getCurrentUser(userDetails);
            BigDecimal cartTotal = cartService.getOrCreateCart(session, userDetails).getTotalPrice();

            int maxSpendable = bonusService.getMaxSpendableForCart(cartTotal, user);

            if (request.getUsedBonuses() > maxSpendable) {
                return ResponseEntity.ok(BonusResponse.error("Нельзя использовать больше " + maxSpendable + " бонусов"));
            }

            BonusAccount account = bonusService.getOrCreateAccount(user);
            if (request.getUsedBonuses() > account.getBalance()) {
                return ResponseEntity.ok(BonusResponse.error("Недостаточно бонусов"));
            }

            int discount = request.getUsedBonuses();
            int finalAmount = cartTotal.intValue() - discount;

            return ResponseEntity.ok(BonusResponse.success(discount, finalAmount));

        } catch (Exception e) {
            log.error("Ошибка расчета бонусов", e);
            return ResponseEntity.ok(BonusResponse.error("Ошибка расчета"));
        }
    }

    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelBonuses(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        try {
            // Проверяем авторизацию
            if (userDetails == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Пользователь не авторизован");
                return ResponseEntity.badRequest().body(response);
            }

            // Сбрасываем использованные бонусы в сессии
            HttpSession session = request.getSession();
            session.removeAttribute("usedBonuses");

            // ✅ Дополнительно: сбрасываем бонусную скидку в сессии
            session.removeAttribute("bonusDiscount");
            session.removeAttribute("bonusApplied");

            // Возвращаем успешный ответ
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Бонусы отменены");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при отмене бонусов", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при отмене бонусов: " + e.getMessage());
            response.put("status", "error");

            return ResponseEntity.badRequest().body(response);
        }
    }
}
