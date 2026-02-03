package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;               // Кто оставил отзыв

    @ManyToOne
    @JoinColumn(name = "dish_id")
    private Dish dish;               // На какое блюдо отзыв

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;             // Отзыв могут оставлять только те, кто действительно заказывал блюдо

    @Min(value = 1, message = "Рейтинг должен быть не менее 1")
    @Max(value = 5, message = "Рейтинг должен быть не более 5")
    private Integer rating;          // 1-5

    @Size(max = 2000, message = "Комментарий не должен превышать 2000 символов")
    private String comment;          // Текстовый отзыв

    private LocalDateTime createdAt;   // дата создания
    private boolean approved = false; // модерация отзыва: true — отзыв проверен и одобрен
}
