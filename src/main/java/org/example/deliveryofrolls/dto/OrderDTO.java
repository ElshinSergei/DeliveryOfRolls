package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.deliveryofrolls.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    @NotBlank(message = "Введите имя")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String customerName;

    @NotBlank(message = "Введите телефон")
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{10,18}$",
            message = "Некорректный формат телефона")
    private String customerPhone;

    @Size(min = 5, max = 200, message = "Адрес должен быть от 5 до 200 символов")
    private String deliveryAddress;

    // ========== НОВЫЕ ПОЛЯ ДЛЯ ДЕТАЛЕЙ АДРЕСА ==========
    @Size(max = 10, message = "Подъезд не более 10 символов")
    private String deliveryEntrance;   // подъезд

    @Size(max = 10, message = "Этаж не более 10 символов")
    private String deliveryFloor;      // этаж

    @Size(max = 10, message = "Квартира не более 10 символов")
    private String deliveryApartment;  // квартира

    @Size(max = 20, message = "Домофон не более 20 символов")
    private String deliveryIntercom;   // домофон
    // ================================================

    private LocalDateTime deliveryTime;

    @Size(max = 500, message = "Комментарий не более 500 символов")
    private String notes;

    @NotNull(message = "Выберите способ оплаты")
    private Order.PaymentMethod paymentMethod;

    @NotNull(message = "Выберите способ получения")
    private Order.DeliveryType deliveryType;

    private String appliedPromoCode;

    @Min(value = 0, message = "Количество бонусов не может быть отрицательным")
    @Max(value = 10000, message = "Слишком много бонусов")
    private Integer usedBonuses = 0;

    private BigDecimal discountAmount;  // сумма скидки по промокоду
    private BigDecimal finalAmount;     // итоговая сумма после скидки

    // конвертация в Order
    public Order toOrder() {
        Order order = new Order();
        order.setCustomerName(this.customerName != null ? this.customerName : "");
        order.setCustomerPhone(this.customerPhone != null ? this.customerPhone : "");
        order.setDeliveryType(this.deliveryType != null ? this.deliveryType : Order.DeliveryType.DELIVERY);
        order.setPaymentMethod(this.paymentMethod != null ? this.paymentMethod : Order.PaymentMethod.CASH);
        order.setNotes(this.notes);
        order.setDeliveryTime(this.deliveryTime);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPromoCode(this.appliedPromoCode);

        // ========== Устанавливаем детали адреса ==========
        order.setDeliveryEntrance(this.deliveryEntrance);
        order.setDeliveryFloor(this.deliveryFloor);
        order.setDeliveryApartment(this.deliveryApartment);
        order.setDeliveryIntercom(this.deliveryIntercom);
        // ================================================

        // Адрес только для доставки
        if (this.deliveryType == Order.DeliveryType.DELIVERY) {
            order.setDeliveryAddress(this.deliveryAddress != null ? this.deliveryAddress : "");
        }

        return order;
    }
}
