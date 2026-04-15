package org.example.deliveryofrolls.repository;

import org.example.deliveryofrolls.entity.BonusSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BonusSettingsRepository extends JpaRepository<BonusSettings, Long> {

    // Получить настройки (они будут одни, так как в таблице только одна запись)
    default BonusSettings getSettings() {
        return findAll().stream().findFirst().orElse(null);
    }

    // Проверить, есть ли уже настройки
    default boolean hasSettings() {
        return count() > 0;
    }
}
