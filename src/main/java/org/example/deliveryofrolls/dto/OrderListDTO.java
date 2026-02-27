package org.example.deliveryofrolls.dto;

import lombok.Data;
import org.example.deliveryofrolls.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class OrderListDTO {
    private Long id;
    private String customerName;
    private String customerPhone;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private String deliveryType;
    private String paymentMethod;
    private boolean hasUser;

    // Статический метод для конвертации
    public static OrderListDTO fromEntity(Order order) {
        OrderListDTO dto = new OrderListDTO();
        dto.setId(order.getId());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setDeliveryType(order.getDeliveryType() != null ?
                order.getDeliveryType().name() : "");
        dto.setPaymentMethod(order.getPaymentMethod() != null ?
                order.getPaymentMethod().name() : "");
        dto.setHasUser(order.getUser() != null);
        return dto;
    }
}
