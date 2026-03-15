package org.example.deliveryofrolls.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "Введите текущий пароль")
    private String oldPassword;

    @NotBlank(message = "Введите новый пароль")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String newPassword;

    @NotBlank(message = "Подтвердите пароль")
    private String confirmPassword;
}
