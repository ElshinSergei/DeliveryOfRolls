package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {

    List<DeliveryZone> findByActiveTrueOrderByDisplayOrderAsc();

    List<DeliveryZone> findAllByOrderByDisplayOrderAsc();
}
