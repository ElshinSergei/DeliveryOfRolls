package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Поиск по email
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя с таким email
    boolean existsByEmail(String email);

    // Количество новых пользователей сегодня
    Long countByRegisteredAtAfter(LocalDateTime startOfDay);

    // Найти пользователей по дню рождения (месяц и день)
    @Query("SELECT u FROM User u WHERE MONTH(u.birthDate) = :month AND DAY(u.birthDate) = :day AND u.birthDate IS NOT NULL")
    List<User> findUsersByBirthDate(@Param("month") int month, @Param("day") int day);

    // Сбросить флаги начисления бонусов
    @Modifying
    @Query("UPDATE User u SET u.birthdayBonusGranted = false")
    void resetAllBirthdayBonusFlags();

}

