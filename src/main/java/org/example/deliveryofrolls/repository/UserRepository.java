package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Поиск по email
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя с таким email
    boolean existsByEmail(String email);

    // Количество новых пользователей сегодня
    Long countByRegisteredAtAfter(LocalDateTime startOfDay);

}

