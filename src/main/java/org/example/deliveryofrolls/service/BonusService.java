package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.repository.BonusAccountRepository;
import org.example.deliveryofrolls.repository.BonusTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BonusService {

    private final BonusAccountRepository accountRepository;
    private final BonusTransactionRepository transactionRepository;
    private final BonusSettingsService settingsService;
    private final UserService userService;


    // Получить или создать бонусный счет
    @Transactional
    public BonusAccount getOrCreateAccount(User user) {
        return accountRepository.findByUserId(user.getId())
                .orElseGet(() -> createAccount(user));
    }

    private BonusAccount createAccount(User user) {
        BonusAccount account = new BonusAccount();
        account.setUser(user);
        account.setBalance(0);
        account = accountRepository.save(account);

        // Используем настройки для приветственных бонусов
        BonusSettings settings = settingsService.getSettings();
        if (settings.isEnabled() && settings.getRegistrationBonus() > 0) {
            addBonus(account, settings.getRegistrationBonus(),
                    BonusTransaction.TransactionType.REGISTRATION,
                    "Приветственный бонус за регистрацию", null);
        }

        log.info("Создан бонусный счет для пользователя {}", user.getEmail());
        return account;
    }

    // Начислить бонусы за заказ
    @Transactional
    public void earnBonusForOrder(Order order) {
        if (order.getUser() == null) {
            return; // гости не получают бонусы
        }

        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) {
            return; // бонусная система отключена
        }

        BonusAccount account = getOrCreateAccount(order.getUser());

        // Рассчитываем бонусы (округляем до целого)
        int bonusAmount = order.getTotalPrice().multiply(
                BigDecimal.valueOf(settings.getEarnPercent())
        ).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();

        if (bonusAmount <= 0) return;

        addBonus(account, bonusAmount, BonusTransaction.TransactionType.ORDER_EARN,
                String.format("Начисление за заказ №%d", order.getId()), order);

        log.info("Пользователю {} начислено {} бонусов за заказ №{}",
                order.getUser().getEmail(), bonusAmount, order.getId());
    }

    // Проверить возможность списания
    public int getMaxSpendableForCart(BigDecimal cartTotal, User user) {
        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) return 0;

        // Проверяем минимальную сумму заказа
        if (cartTotal.compareTo(settings.getMinOrderAmount()) < 0) {
            return 0;
        }

        BonusAccount account = getOrCreateAccount(user);
        int maxByBalance = account.getBalance();
        int maxByCart = cartTotal.multiply(
                BigDecimal.valueOf(settings.getMaxSpendPercent())
        ).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();

        return Math.min(maxByBalance, maxByCart);
    }

    // Списать бонусы при заказе
    @Transactional
    public boolean spendBonusForOrder(User user, int bonusToSpend) {
        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) return false;

        BonusAccount account = getOrCreateAccount(user);

        if (account.getBalance() < bonusToSpend) {
            return false;
        }

        account.setBalance(account.getBalance() - bonusToSpend);
        account.setTotalSpent(account.getTotalSpent() + bonusToSpend);
        accountRepository.save(account);

        BonusTransaction transaction = new BonusTransaction();
        transaction.setAccount(account);
        transaction.setAmount(-bonusToSpend);
        transaction.setType(BonusTransaction.TransactionType.ORDER_SPEND);
        transaction.setDescription("Списание бонусов при заказе");
        transaction.setOrder(null);
        transactionRepository.save(transaction);

        log.info("Списано {} бонусов у пользователя {}", bonusToSpend, user.getEmail());
        return true;
    }

    // Начисление бонусов за день рождения
    @Transactional
    public void addBirthdayBonus(User user) {
        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled() || settings.getBirthdayBonus() <= 0) return;

        BonusAccount account = getOrCreateAccount(user);
        addBonus(account, settings.getBirthdayBonus(),
                BonusTransaction.TransactionType.BIRTHDAY,
                "Бонус ко дню рождения", null);
    }

    private void addBonus(BonusAccount account, int amount, BonusTransaction.TransactionType type,
                          String description, Order order) {
        account.setBalance(account.getBalance() + amount);
        account.setTotalEarned(account.getTotalEarned() + amount);

        BonusTransaction transaction = new BonusTransaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setOrder(order);
        transactionRepository.save(transaction);

        accountRepository.save(account);
    }

    // Получить историю транзакций
    public List<BonusTransaction> getTransactionHistory(User user) {
        BonusAccount account = getOrCreateAccount(user);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId());
    }

    // Рассчитать количество бонусов к начислению
    public int calculateEarnedBonuses(BigDecimal orderAmount) {
        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) return 0;

        return orderAmount.multiply(
                BigDecimal.valueOf(settings.getEarnPercent())
        ).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
    }

    // ВОЗВРАТ БОНУСОВ ПРИ ОТМЕНЕ ЗАКАЗА
    @Transactional
    public void refundBonuses(Order order) {
        if (order.getUser() == null || order.getBonusUsed() == null || order.getBonusUsed() <= 0) {
            return;
        }

        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) return;

        BonusAccount account = getOrCreateAccount(order.getUser());

        // Возвращаем бонусы на счет
        account.setBalance(account.getBalance() + order.getBonusUsed());
        account.setTotalSpent(account.getTotalSpent() - order.getBonusUsed());

        // Создаем транзакцию возврата
        BonusTransaction transaction = new BonusTransaction();
        transaction.setAccount(account);
        transaction.setAmount(order.getBonusUsed());
        transaction.setType(BonusTransaction.TransactionType.REFUND);
        transaction.setDescription(String.format("Возврат бонусов за отмененный заказ №%d", order.getId()));
        transaction.setOrder(order);
        transactionRepository.save(transaction);

        accountRepository.save(account);

        log.info("Возвращено {} бонусов пользователю {} за отмененный заказ №{}",
                order.getBonusUsed(), order.getUser().getEmail(), order.getId());
    }

    // СПИСАНИЕ НАЧИСЛЕННЫХ БОНУСОВ ПРИ ОТМЕНЕ ЗАКАЗА
    @Transactional
    public void deductEarnedBonuses(Order order) {
        if (order.getUser() == null || order.getBonusEarned() == null || order.getBonusEarned() <= 0) {
            return;
        }

        BonusSettings settings = settingsService.getSettings();
        if (!settings.isEnabled()) return;

        BonusAccount account = getOrCreateAccount(order.getUser());

        // Проверяем, достаточно ли бонусов на счете
        if (account.getBalance() < order.getBonusEarned()) {
            log.warn("Недостаточно бонусов для списания за отмененный заказ #{}. Баланс: {}, нужно: {}",
                    order.getId(), account.getBalance(), order.getBonusEarned());
            return;
        }

        // Списываем начисленные бонусы
        account.setBalance(account.getBalance() - order.getBonusEarned());
        account.setTotalEarned(account.getTotalEarned() - order.getBonusEarned());

        // Создаем транзакцию списания
        BonusTransaction transaction = new BonusTransaction();
        transaction.setAccount(account);
        transaction.setAmount(-order.getBonusEarned());
        transaction.setType(BonusTransaction.TransactionType.REFUND);
        transaction.setDescription(String.format("Списание бонусов за отмененный заказ №%d", order.getId()));
        transaction.setOrder(order);
        transactionRepository.save(transaction);

        accountRepository.save(account);

        log.info("Списано {} начисленных бонусов у пользователя {} за отмененный заказ №{}",
                order.getBonusEarned(), order.getUser().getEmail(), order.getId());
    }

    // Получить количество активных пользователей (у кого есть бонусы)
    @Transactional(readOnly = true)
    public long getActiveBonusUsersCount() {
        return accountRepository.countByBalanceGreaterThan();
    }

    // Получить общий баланс всех бонусов
    @Transactional(readOnly = true)
    public long getTotalBonusBalance() {
        return accountRepository.getTotalBonusBalance();
    }
}