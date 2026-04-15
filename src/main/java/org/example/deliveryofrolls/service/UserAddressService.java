package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.UserAddressDTO;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.entity.UserAddress;
import org.example.deliveryofrolls.repository.UserAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserAddressService {

    private final UserAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<UserAddressDTO> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                .stream()
                .map(UserAddressDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserAddressDTO getAddress(Long id, User user) {
        UserAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Адрес не найден"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Это не ваш адрес");
        }

        return UserAddressDTO.fromEntity(address);
    }

    public UserAddressDTO saveAddress(UserAddressDTO addressDTO, User user) {
        UserAddress address = addressDTO.toEntity();
        address.setUser(user);

        // Если адрес помечен как основной, сбрасываем основной у других адресов
        if (address.isDefault()) {
            addressRepository.resetDefaultAddress(user);
        }

        UserAddress saved = addressRepository.save(address);
        log.info("Адрес сохранен для пользователя {}: {}", user.getId(), saved.getFullAddress());

        return UserAddressDTO.fromEntity(saved);
    }

    public void deleteAddress(Long id, User user) {
        UserAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Адрес не найден"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Это не ваш адрес");
        }

        addressRepository.delete(address);
        log.info("Адрес удален для пользователя {}: {}", user.getId(), address.getFullAddress());
    }

    public UserAddressDTO setDefaultAddress(Long id, User user) {
        // Сбрасываем основной адрес у всех
        addressRepository.resetDefaultAddress(user);

        // Устанавливаем новый основной
        UserAddress address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Адрес не найден"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Это не ваш адрес");
        }

        address.setDefault(true);
        UserAddress saved = addressRepository.save(address);

        return UserAddressDTO.fromEntity(saved);
    }

    /**
     * Проверить, существует ли адрес в избранном у пользователя
     */
    @Transactional(readOnly = true)
    public boolean addressExists(User user, String address) {
        return addressRepository.existsByUserAndAddress(user, address);
    }

    /**
     * Сохранить адрес из checkout (без DTO)
     */
    public UserAddressDTO saveAddressFromCheckout(User user, String address, String entrance,
                                                  String floor, String apartment, String comment,
                                                  boolean isDefault) {
        // Проверяем, не существует ли уже такой адрес
        if (addressExists(user, address)) {
            log.info("Адрес уже существует в избранном: {}", address);
            // Возвращаем существующий адрес
            UserAddress existing = addressRepository.findByUserAndAddress(user, address)
                    .orElseThrow(() -> new IllegalArgumentException("Адрес не найден"));
            return UserAddressDTO.fromEntity(existing);
        }

        UserAddressDTO addressDTO = new UserAddressDTO();
        addressDTO.setAddress(address);
        addressDTO.setEntrance(entrance);
        addressDTO.setFloor(floor);
        addressDTO.setApartment(apartment);
        addressDTO.setComment(comment);
        addressDTO.setDefault(isDefault);

        return saveAddress(addressDTO, user);
    }
}