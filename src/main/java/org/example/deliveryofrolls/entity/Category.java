package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
    private String imageUrl;
    private Integer sortOrder = 0;  // Определяет порядок отображения категорий в меню

    private boolean available = true;

    @OneToMany(mappedBy = "category")
    private List<Dish> dishes;
}
