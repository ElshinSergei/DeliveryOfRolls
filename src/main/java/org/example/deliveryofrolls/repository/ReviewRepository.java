package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.entity.Review;
import org.example.deliveryofrolls.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Найти отзывы по блюду
    List<Review> findByDish(Dish dish);

    // Найти отзывы пользователя
    List<Review> findByUser(User user);

    // Найти отзывы, требующие модерации
    List<Review> findByApprovedFalse();
}
