package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryZoneRequest {

    @NotBlank(message = "Название зоны обязательно")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String name;

    @NotBlank(message = "Цвет обязателен")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Цвет должен быть в формате HEX (например, #FF0000)")
    private String color;

    @NotBlank(message = "Цвет границы обязателен")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Цвет границы должен быть в формате HEX")
    private String borderColor;

    @NotNull(message = "Прозрачность заливки обязательна")
    @DecimalMin(value = "0.0", message = "Прозрачность не может быть меньше 0")
    @DecimalMax(value = "1.0", message = "Прозрачность не может быть больше 1")
    private Double fillOpacity;

    @NotNull(message = "Минимальная сумма заказа обязательна")
    @Min(value = 0, message = "Минимальная сумма заказа не может быть отрицательной")
    @Max(value = 100000, message = "Минимальная сумма заказа не может превышать 100 000 ₽")
    private Integer minOrder;

    @NotBlank(message = "Время доставки обязательно")
    private String deliveryTime;

    @NotEmpty(message = "Точки полигона обязательны")
    @Size(min = 3, message = "Для зоны доставки нужно минимум 3 точки")
    private List<List<Double>> points;

    private Boolean active = true;
}
