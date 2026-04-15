package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    Optional<UserAddress> findByUserAndAddress(User user, String address);

    boolean existsByUserAndAddress(User user, String address);

    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user = :user")
    void resetDefaultAddress(@Param("user") User user);
}
