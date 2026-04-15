package org.example.deliveryofrolls.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromoCodeResponse {
    private boolean valid;          // true - промокод работает, false - ошибка
    private String message;         // Сообщение для пользователя
    private BigDecimal discountAmount; // Сумма скидки в рублях
    private BigDecimal finalAmount;    // Итоговая сумма после скидки
    private String discountText;    // Текст для отображения (например "10%")


    // Статические методы-фабрики для удобного создания ответов

    public static PromoCodeResponse success(String code, BigDecimal discount, BigDecimal finalAmount, String discountText) {
        PromoCodeResponse response = new PromoCodeResponse();
        response.setValid(true);
        response.setMessage("Промокод " + code + " применен!");
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);
        response.setDiscountText(discountText);
        return response;
    }

    public static PromoCodeResponse error(String message) {
        PromoCodeResponse response = new PromoCodeResponse();
        response.setValid(false);
        response.setMessage(message);
        return response;
    }
}
