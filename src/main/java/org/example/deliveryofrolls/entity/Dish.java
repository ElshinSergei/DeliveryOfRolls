package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private Integer weight; // вес в граммах
    private Integer calories; // калории

    @ElementCollection  // Автоматически создает таблицу для хранения коллекции
    @CollectionTable(name = "dish_ingredients", joinColumns = @JoinColumn(name = "dish_id"))  // Определяет таблицу для хранения коллекции
    @Column(name = "ingredient")
    private List<String> ingredients;

    private String imageUrl;

    private boolean available = true;    // доступно ли для заказа

    @ManyToMany
    @JoinTable(
            name = "dish_options",
            joinColumns = @JoinColumn(name = "dish_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id")
    )
    private List<DishOption> availableOptions;   // доп опции к блюду

    private LocalDateTime createdAt;      // Когда блюдо было добавлено в меню
}
