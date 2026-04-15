package org.example.deliveryofrolls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZoneResponse {
    private Long id;
    private String name;
    private String color;
    private String borderColor;
    private Double fillOpacity;
    private Integer minOrder;
    private String deliveryTime;
    private List<List<Double>> points;
    private Boolean active;
    private Integer displayOrder;
}
