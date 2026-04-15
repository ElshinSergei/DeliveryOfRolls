package org.example.deliveryofrolls.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.ProfileDTO;
import org.example.deliveryofrolls.dto.RegisterDTO;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Получение текущего пользователя из UserDetails
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

    // Получение текущего пользователя по email
    public User getCurrentUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

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

        User savedUser = userRepository.save(user);

        // ========== ОТПРАВКА ПРИВЕТСТВЕННОГО ПИСЬМА ==========
        try {
            emailService.sendWelcomeEmail(savedUser, 100);
            log.info("✅ Приветственное письмо отправлено на {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("❌ Не удалось отправить приветственное письмо на {}: {}",
                    savedUser.getEmail(), e.getMessage());
            // Не бросаем исключение - регистрация уже прошла успешно
        }
        // ====================================================
    }

    // Обновление
    public void updateProfile(Long userId, ProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setPhone(profileDTO.getPhone());
        user.setBirthDate(profileDTO.getBirthDate());
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

        // Проверяем, что новый пароль не совпадает со старым
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Пароль изменен для пользователя: {}", user.getEmail());
    }

    // Фильтрация
    public Page<User> findUsersByFilters(String search, String role, Boolean enabled, Pageable pageable) {

        Specification<User> spec = Specification.where(null);

        // Поиск по email, имени, телефону
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("email")), searchPattern),
                            cb.like(cb.lower(root.get("firstName")), searchPattern),
                            cb.like(cb.lower(root.get("lastName")), searchPattern),
                            cb.like(root.get("phone"), searchPattern)
                    ));
        }

        // Фильтр по роли
        if (role != null && !role.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), User.Role.valueOf(role)));
        }

        // Фильтр по статусу (активен/заблокирован)
        if (enabled != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("enabled"), enabled));
        }

        return userRepository.findAll(spec, pageable);
    }

    // БЛОКИРОВКА/РАЗБЛОКИРОВКА
    public void toggleUserStatus(Long userId, User currentUser) {
        // Проверка на самого себя
        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }

        User user = getUserById(userId);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);

        log.info("Пользователь {} {} пользователем {}",
                user.getEmail(),
                user.isEnabled() ? "разблокирован" : "заблокирован",
                currentUser.getEmail());
    }

    // Получить статус пользователя для сообщения
    public String getUserStatusMessage(User user) {
        return user.isEnabled() ? "разблокирован" : "заблокирован";
    }

    // Создание пользователя администратором
    @Transactional
    public User createUserByAdmin(RegisterDTO userDTO, User.Role role) {
        // Проверка на существующий email
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setRole(role);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        return userRepository.save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }
}
