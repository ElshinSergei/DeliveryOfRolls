package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Data
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String address; // Полный адрес

    private String entrance; // Подъезд

    private String floor; // Этаж

    private String apartment; // Квартира/офис

    private String comment; // Комментарий к адресу (домофон, код и т.д.)

    @Column(name = "is_default")
    private boolean isDefault = false; // Адрес по умолчанию

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullAddress() {
        StringBuilder full = new StringBuilder(address);
        if (entrance != null && !entrance.isEmpty()) {
            full.append(", подъезд ").append(entrance);
        }
        if (floor != null && !floor.isEmpty()) {
            full.append(", этаж ").append(floor);
        }
        if (apartment != null && !apartment.isEmpty()) {
            full.append(", кв ").append(apartment);
        }
        return full.toString();
    }
}
