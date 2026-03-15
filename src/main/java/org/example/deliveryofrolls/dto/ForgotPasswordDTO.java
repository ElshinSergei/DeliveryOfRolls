package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "Введите email")
    @Email(message = "Неверный формат email")
    private String email;

}
