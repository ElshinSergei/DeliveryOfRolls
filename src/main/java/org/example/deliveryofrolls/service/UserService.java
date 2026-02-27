package org.example.deliveryofrolls.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.ProfileDTO;
import org.example.deliveryofrolls.dto.RegisterDTO;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Получение текущего пользователя
    public User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    // Получение текущего пользователя по ID
    public User getUserById(Long id) {
       return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    // регистрация
    public void registerUser(RegisterDTO registerDTO) {

        if(userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + registerDTO.getEmail() + " уже существует");
        }

        User user = new User();
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setFirstName(registerDTO.getFirstName());
        user.setLastName(registerDTO.getLastName());
        user.setPhone(registerDTO.getPhone());

        // Значения по умолчанию
        user.setRole(User.Role.ROLE_USER);
        user.setEnabled(true);

        userRepository.save(user);
    }

    // Обновление
    public void updateProfile(Long userId, ProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setPhone(profileDTO.getPhone());
        userRepository.save(user);
    }

    // Смена пароля
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Проверка старого пароля
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

}
