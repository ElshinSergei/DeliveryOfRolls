package org.example.deliveryofrolls.dto;

import lombok.Data;

@Data
public class PromoCodeInfo {
    private String code;           // Код для ввода (WELCOME10)
    private String description;    // Описание акции
    private String discountText;   // Текст скидки
    private String timeInfo;       // Информация о времени действия (для счастливых часов)
}
