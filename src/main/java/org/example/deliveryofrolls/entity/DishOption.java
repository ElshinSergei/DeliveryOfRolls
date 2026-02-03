package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dish_options")
public class DishOption {        // Дополнение для блюда
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;   // "Дополнительный соус", "Имбирь", "Васаби"
    private String description;
    private BigDecimal additionalPrice;  // дополнительная цена
    private boolean defaultIncluded;    // включено по умолчанию?

    @ManyToMany(mappedBy = "availableOptions")
    private List<Dish> dishes;
}
