package org.example.deliveryofrolls.dto;

import lombok.Data;
import org.example.deliveryofrolls.entity.UserAddress;

@Data
public class UserAddressDTO {
    private Long id;
    private String address;
    private String entrance;
    private String floor;
    private String apartment;
    private String comment;
    private boolean isDefault;
    private String fullAddress;

    public static UserAddressDTO fromEntity(UserAddress address) {
        UserAddressDTO dto = new UserAddressDTO();
        dto.setId(address.getId());
        dto.setAddress(address.getAddress());
        dto.setEntrance(address.getEntrance());
        dto.setFloor(address.getFloor());
        dto.setApartment(address.getApartment());
        dto.setComment(address.getComment());
        dto.setDefault(address.isDefault());
        dto.setFullAddress(address.getFullAddress());
        return dto;
    }

    public UserAddress toEntity() {
        UserAddress address = new UserAddress();
        address.setId(this.id);
        address.setAddress(this.address);
        address.setEntrance(this.entrance);
        address.setFloor(this.floor);
        address.setApartment(this.apartment);
        address.setComment(this.comment);
        address.setDefault(this.isDefault);
        return address;
    }
}
