package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Поиск по email
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя с таким email
    boolean existsByEmail(String email);

    // Количество новых пользователей сегодня
    Long countByRegisteredAtAfter(LocalDateTime startOfDay);

    // Список всех пользователей с сортировкой по дате регистрации
    List<User> findAllByOrderByRegisteredAtDesc();

}
