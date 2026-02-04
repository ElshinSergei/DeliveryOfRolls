package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Поиск по email
    Optional<User> findByEmail(String email);

    // Проверка существования пользователя с таким email
    boolean existsByEmail(String email);

    // Найти всех с определенной ролью
    List<User> findAllByRole(User.Role role);

    // Найти по телефону
    Optional<User> findByPhone(String phone);

    // Поиск по имени и фамилии
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);

}
