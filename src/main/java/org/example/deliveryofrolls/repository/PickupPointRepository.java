package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.PickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickupPointRepository extends JpaRepository<PickupPoint, Long> {

    List<PickupPoint> findByActiveTrueOrderByDisplayOrderAsc();

    List<PickupPoint> findAllByOrderByDisplayOrderAsc();
}
