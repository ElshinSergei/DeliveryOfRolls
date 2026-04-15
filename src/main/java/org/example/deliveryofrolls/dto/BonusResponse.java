package org.example.deliveryofrolls.dto;

import lombok.Data;

@Data
public class BonusResponse {
    private boolean valid;
    private String message;
    private Integer discountAmount;
    private Integer finalAmount;

    public static BonusResponse success(int discount, int finalAmount) {
        BonusResponse response = new BonusResponse();
        response.setValid(true);
        response.setMessage("Бонусы применены");
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);
        return response;
    }

    public static BonusResponse error(String message) {
        BonusResponse response = new BonusResponse();
        response.setValid(false);
        response.setMessage(message);
        return response;
    }
}
