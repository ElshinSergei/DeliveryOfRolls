package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PickupPointRequest {

    @NotBlank(message = "Название обязательно")
    private String name;

    @NotBlank(message = "Адрес обязателен")
    private String address;

    private String coordinates;

    private String workingHours;

    private String phone;

    private Boolean active = true;

    private Integer displayOrder = 0;

    private String description;
}
