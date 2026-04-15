package org.example.deliveryofrolls.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickupPointResponse {
    private Long id;
    private String name;
    private String address;
    private String coordinates;
    private String workingHours;
    private String phone;
    private Boolean active;
    private Integer displayOrder;
    private String description;
}
