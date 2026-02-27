package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.deliveryofrolls.entity.Order;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    @NotBlank(message = "Введите имя")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String customerName;

    @NotBlank(message = "Введите телефон")
    @Pattern(regexp = "^\\+?[78][-\\(]?\\d{3}\\)?-?\\d{3}-?\\d{2}-?\\d{2}$",
            message = "Некорректный формат телефона")
    private String customerPhone;

    @Size(min = 5, max = 200, message = "Адрес должен быть от 5 до 200 символов")
    private String deliveryAddress;

    private LocalDateTime deliveryTime;

    @Size(max = 500, message = "Комментарий не более 500 символов")
    private String notes;

    @NotNull(message = "Выберите способ оплаты")
    private Order.PaymentMethod paymentMethod;

    @NotNull(message = "Выберите способ получения")
    private Order.DeliveryType deliveryType;

    // конвертация в Order
    public Order toOrder() {
        Order order = new Order();
        order.setCustomerName(this.customerName);
        order.setCustomerPhone(this.customerPhone);
        order.setDeliveryType(this.deliveryType);
        order.setPaymentMethod(this.paymentMethod);
        order.setNotes(this.notes);
        order.setDeliveryTime(this.deliveryTime);
        order.setStatus(Order.OrderStatus.PENDING);

        // Адрес только для доставки
        if (this.deliveryType == Order.DeliveryType.DELIVERY) {
            order.setDeliveryAddress(this.deliveryAddress);
        }

        return order;
    }
}
