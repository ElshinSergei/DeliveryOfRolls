package org.example.deliveryofrolls.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.deliveryofrolls.converter.PointsJsonConverter;

import java.util.List;

@Entity
@Table(name = "delivery_zones")
@Data
@NoArgsConstructor
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String borderColor;

    @Column(nullable = false)
    private Double fillOpacity;

    @Column(nullable = false)
    private Integer minOrder;

    @Column(nullable = false)
    private String deliveryTime;

    @Convert(converter = PointsJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<List<Double>> points;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;
}
