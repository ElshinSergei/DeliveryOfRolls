package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pickup_points")
@Data
public class PickupPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    private String coordinates; // "lng,lat" формат

    @Column(name = "working_hours")
    private String workingHours;

    private String phone;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    private String description;
}
