package org.example.deliveryofrolls.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.BonusSettings;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.UserRepository;
import org.example.deliveryofrolls.service.BonusService;
import org.example.deliveryofrolls.service.BonusSettingsService;
import org.example.deliveryofrolls.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayBonusScheduler {

    private final UserRepository userRepository;
    private final BonusService bonusService;
    private final EmailService emailService;
    private final BonusSettingsService bonusSettingsService;

    /**
     * Проверка дней рождения каждый день в 10:00
     */
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void checkBirthdaysAndGrantBonus() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        log.info("🎂 Проверка дней рождения: {}.{}", currentDay, currentMonth);

        // Находим пользователей с днём рождения сегодня
        List<User> birthdayUsers = userRepository.findUsersByBirthDate(currentMonth, currentDay);

        int grantedCount = 0;
        for (User user : birthdayUsers) {
            try {
                // Проверяем, не начисляли ли бонус в этом году
                if (!user.isBirthdayBonusGranted()) {
                    bonusService.addBirthdayBonus(user);

                    // Отправляем поздравительное письмо
                    BonusSettings settings = bonusSettingsService.getSettings();
                    int birthdayBonusAmount = settings.getBirthdayBonus() != null ? settings.getBirthdayBonus() : 200;
                    emailService.sendBirthdayGreeting(user, birthdayBonusAmount);

                    user.setBirthdayBonusGranted(true);
                    userRepository.save(user);
                    grantedCount++;
                    log.info("✅ Начислен бонус ко дню рождения пользователю: {}", user.getEmail());
                } else {
                    log.debug("Бонус уже начислен пользователю {} в этом году", user.getEmail());
                }
            } catch (Exception e) {
                log.error("❌ Ошибка начисления бонуса пользователю {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("📊 Начислено бонусов ко дню рождения: {}", grantedCount);
    }

    /**
     * Сброс флагов в новом году (1 января в 00:00)
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void resetBirthdayBonuses() {
        userRepository.resetAllBirthdayBonusFlags();
        log.info("🔄 Сброшены флаги начисления бонусов за день рождения на новый год");
    }
}
