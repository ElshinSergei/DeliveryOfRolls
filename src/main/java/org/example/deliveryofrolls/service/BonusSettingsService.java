package org.example.deliveryofrolls.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.BonusSettings;
import org.example.deliveryofrolls.repository.BonusSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BonusSettingsService {

    private final BonusSettingsRepository settingsRepository;

    @PostConstruct
    public void init() {
        if (settingsRepository.count() == 0) {
            BonusSettings settings = new BonusSettings();
            settingsRepository.save(settings);
        }
    }

    public BonusSettings getSettings() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    BonusSettings settings = new BonusSettings();
                    return settingsRepository.save(settings);
                });
    }

    public void updateSettings(BonusSettings settings) {
        BonusSettings existing = getSettings();
        existing.setEarnPercent(settings.getEarnPercent());
        existing.setMaxSpendPercent(settings.getMaxSpendPercent());
        existing.setRegistrationBonus(settings.getRegistrationBonus());
        existing.setBirthdayBonus(settings.getBirthdayBonus());
        existing.setMinOrderAmount(settings.getMinOrderAmount());
        existing.setBonusExpiryDays(settings.getBonusExpiryDays());
        existing.setEnabled(settings.isEnabled());
        settingsRepository.save(existing);
    }

}
