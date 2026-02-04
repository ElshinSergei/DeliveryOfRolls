package org.example.deliveryofrolls.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // регистрация
    public User registerUser(String email, String password, String firstName,
                             String lastName, String phone) {
        if(userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Пользователь с email " + email + " уже существует");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Хеширование!
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setRole(User.Role.ROLE_USER); // По умолчанию обычный пользователь
        user.setEnabled(true);             // Активирован сразу
        user.setRegisteredAt(LocalDateTime.now()); // Дата регистрации
        return userRepository.save(user);
    }

    // Поиск по email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Проверка существования
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Обновление
    public User updateProfile(Long userId, String firstName,
                              String lastName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        return userRepository.save(user);
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

    // Получение по ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }
}
