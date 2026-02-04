package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.DishOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishOptionRepository extends JpaRepository<DishOption, Long> {

    // Найти опции по названию
    List<DishOption> findByName(String name);

}
