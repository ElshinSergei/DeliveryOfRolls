package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.OrderItem;
import org.example.deliveryofrolls.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // Внедряем значения из properties
    @Value("${app.email.admin}")
    private String adminEmail;

    // ОТПРАВКА ВРЕМЕННОГО ПАРОЛЯ
    public void sendPasswordResetEmail(String to, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // От кого
            message.setFrom(adminEmail);
            // Кому
            message.setTo(to);
            // Тема письма
            message.setSubject("Восстановление пароля - RedRolls");
            // Текст письма
            message.setText(
                    "Здравствуйте!\n\n" +
                            "Вы запросили восстановление пароля на сайте RedRolls.\n\n" +
                            "Ваш временный пароль: " + tempPassword + "\n\n" +
                            "Рекомендуем сменить его после входа в личном кабинете.\n\n" +
                            "Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.\n\n" +
                            "С уважением,\n" +
                            "Команда RedRolls"
            );
            // Отправляем
            mailSender.send(message);

            log.info("Письмо с паролем отправлено на {}", to);

        } catch (Exception e) {
            log.error("Ошибка отправки письма: {}", e.getMessage());
            throw new RuntimeException("Не удалось отправить письмо", e);
        }
    }

    // ОТПРАВКА ДЕТАЛЕЙ ЗАКАЗА КЛИЕНТУ
    public void sendOrderConfirmationToCustomer(Order order) {
        if (order.getUser() == null) {
            log.info("Заказ #{} от гостя - email не отправляем", order.getId());
            return;
        }

        String email = order.getUser().getEmail();
        if (email == null || email.isEmpty()) {
            log.warn("У пользователя заказа #{} нет email", order.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(email);
            message.setSubject("✅ Ваш заказ #" + order.getId() + " принят!");
            message.setText(formatOrderForCustomer(order));

            mailSender.send(message);
            log.info("Подтверждение заказа #{} отправлено на {}", order.getId(), email);

        } catch (Exception e) {
            log.error("Ошибка отправки подтверждения заказа #{}: {}", order.getId(), e.getMessage());
        }
    }

    // ПРИВЕТСТВЕННОЕ ПИСЬМО ПРИ РЕГИСТРАЦИИ
    public void sendWelcomeEmail(User user, int bonusAmount) {
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            log.warn("У пользователя {} нет email", user.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(email);
            message.setSubject("Добро пожаловать в RedRolls! 🎉");
            message.setText(formatWelcomeMessage(user, bonusAmount));

            mailSender.send(message);
            log.info("Приветственное письмо отправлено на {}", email);

        } catch (Exception e) {
            log.error("Ошибка отправки приветственного письма для {}: {}", email, e.getMessage());
        }
    }

    // Форматирование приветственного письма
    private String formatWelcomeMessage(User user, int bonusAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте, ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("!\n\n");
        sb.append("🎉 Добро пожаловать в RedRolls!\n\n");
        sb.append("Благодарим вас за регистрацию на нашем сайте.\n\n");

        sb.append("✨ ВАШ ПОДАРОК:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("🎁 На ваш счет начислено %d бонусов!\n", bonusAmount));
        sb.append("💡 1 бонус = 1 рубль\n");
        sb.append("💰 Бонусы можно использовать для оплаты до 30% от суммы заказа\n\n");

        sb.append("🍣 ЧТО ДАЛЬШЕ?\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("1. Выберите любимые роллы в нашем меню\n");
        sb.append("2. Оформите заказ с доставкой или самовывозом\n");
        sb.append("3. Используйте бонусы для оплаты\n\n");

        sb.append("🔗 Быстрый переход в меню:\n");
        sb.append("https://redrolls.ru/\n\n");

        sb.append("Если у вас есть вопросы, мы всегда на связи:\n");
        sb.append("📞 +7 (999) 123-45-67\n");
        sb.append("✉️ support@redrolls.ru\n\n");

        sb.append("С уважением,\n");
        sb.append("Команда RedRolls 🍣");

        return sb.toString();
    }

    public void sendOrderNotificationToKitchen(Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(adminEmail);
            message.setSubject("🔔 НОВЫЙ ЗАКАЗ #" + order.getId());
            message.setText(formatOrderForKitchen(order));

            mailSender.send(message);
            log.info("Заказ #{} отправлен на кухню", order.getId());

        } catch (Exception e) {
            log.error("Ошибка отправки заказа #{} на кухню: {}", order.getId(), e.getMessage());
        }
    }

    private String formatOrderForCustomer(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Здравствуйте, ").append(order.getCustomerName()).append("!\n\n");
        sb.append("✅ Ваш заказ #").append(order.getId()).append(" принят и передан в работу!\n\n");

        sb.append("🍣 СОСТАВ ЗАКАЗА:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        // Проверяем, есть ли товары в заказе
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                sb.append(String.format("%s x%d - %d ₽\n",
                        item.getDishName(),
                        item.getQuantity(),
                        item.getTotalPrice().intValue()
                ));
            }
        } else {
            sb.append("Нет товаров в заказе\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        if (order.getDiscountAmount() != null && order.getDiscountAmount().intValue() > 0) {
            sb.append(String.format("Скидка по промокоду: -%d ₽\n",
                    order.getDiscountAmount().intValue()));
        }

        if (order.getBonusUsed() != null && order.getBonusUsed() > 0) {
            sb.append(String.format("Скидка бонусами: -%d ₽\n", order.getBonusUsed()));
        }

        sb.append(String.format("💰 ИТОГО: %d ₽\n\n", order.getTotalPrice().intValue()));

        sb.append("📍 ДЕТАЛИ ДОСТАВКИ:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        if (order.getDeliveryType() == Order.DeliveryType.DELIVERY) {
            sb.append("Тип: Доставка\n");
            sb.append("🏠 Адрес: ").append(order.getDeliveryAddress()).append("\n");

            // ✅ ДОБАВЛЯЕМ ДЕТАЛИ АДРЕСА
            if (order.getDeliveryEntrance() != null && !order.getDeliveryEntrance().isEmpty()) {
                sb.append("🚪 Подъезд: ").append(order.getDeliveryEntrance()).append("\n");
            }
            if (order.getDeliveryFloor() != null && !order.getDeliveryFloor().isEmpty()) {
                sb.append("🔼 Этаж: ").append(order.getDeliveryFloor()).append("\n");
            }
            if (order.getDeliveryApartment() != null && !order.getDeliveryApartment().isEmpty()) {
                sb.append("🏠 Квартира: ").append(order.getDeliveryApartment()).append("\n");
            }
            if (order.getDeliveryIntercom() != null && !order.getDeliveryIntercom().isEmpty()) {
                sb.append("📞 Домофон: ").append(order.getDeliveryIntercom()).append("\n");
            }
        } else {
            sb.append("Тип: Самовывоз\n");
            sb.append("🏪 Адрес самовывоза: г. Пермь, ул. Ленина, 10\n");
        }

        if (order.getDeliveryTime() != null) {
            sb.append("⏰ Время: ").append(order.getDeliveryTime().toString()).append("\n");
        } else {
            sb.append("⏰ Время: как можно скорее\n");
        }

        sb.append("💳 Оплата: ").append(getPaymentMethodText(order.getPaymentMethod())).append("\n\n");

        sb.append("📱 Следить за статусом заказа можно в личном кабинете:\n");
        sb.append("https://redrolls.ru/profile/orders/").append(order.getId()).append("\n\n");

        sb.append("С уважением,\n");
        sb.append("Команда RedRolls 🍣");

        return sb.toString();
    }

    private String formatOrderForKitchen(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 НОВЫЙ ЗАКАЗ #").append(order.getId()).append("\n");
        sb.append("⏰ Время: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");

        if (order.getUser() != null) {
            sb.append("👤 Клиент: ").append(order.getCustomerName()).append(" (авторизован)\n");
        } else {
            sb.append("👤 Клиент (гость): ").append(order.getCustomerName()).append("\n");
        }

        sb.append("📞 Телефон: ").append(order.getCustomerPhone()).append("\n\n");

        sb.append("🍣 КУХНЯ:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                sb.append(String.format("• %s x%d\n",
                        item.getDishName(),
                        item.getQuantity()
                ));
                if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isEmpty()) {
                    sb.append("  📝 ").append(item.getSpecialInstructions()).append("\n");
                }
            }
        } else {
            sb.append("❌ ОШИБКА: Нет товаров в заказе!\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("📍 ДОСТАВКА:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");

        if (order.getDeliveryType() == Order.DeliveryType.DELIVERY) {
            sb.append("Тип: Доставка\n");
            sb.append("🏠 Адрес: ").append(order.getDeliveryAddress()).append("\n");

            // ✅ ДОБАВЛЯЕМ ДЕТАЛИ АДРЕСА
            if (order.getDeliveryEntrance() != null && !order.getDeliveryEntrance().isEmpty()) {
                sb.append("🚪 Подъезд: ").append(order.getDeliveryEntrance()).append("\n");
            }
            if (order.getDeliveryFloor() != null && !order.getDeliveryFloor().isEmpty()) {
                sb.append("🔼 Этаж: ").append(order.getDeliveryFloor()).append("\n");
            }
            if (order.getDeliveryApartment() != null && !order.getDeliveryApartment().isEmpty()) {
                sb.append("🏠 Квартира: ").append(order.getDeliveryApartment()).append("\n");
            }
            if (order.getDeliveryIntercom() != null && !order.getDeliveryIntercom().isEmpty()) {
                sb.append("📞 Домофон: ").append(order.getDeliveryIntercom()).append("\n");
            }
        } else {
            sb.append("Тип: Самовывоз\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (order.getDeliveryTime() != null) {
            sb.append("⏰ Время доставки: ").append(
                    order.getDeliveryTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
            ).append("\n");
        } else {
            sb.append("⏰ Время доставки: как можно скорее\n");
        }

        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            sb.append("📝 Комментарий: ").append(order.getNotes()).append("\n");
        }

        sb.append("\n💳 ОПЛАТА: ").append(getPaymentMethodText(order.getPaymentMethod())).append("\n");

        // Добавляем информацию о скидках
        if (order.getDiscountAmount() != null && order.getDiscountAmount().intValue() > 0) {
            sb.append("🎫 Промокод: ").append(order.getPromoCode()).append(" (-").append(order.getDiscountAmount()).append(" ₽)\n");
        }
        if (order.getBonusUsed() != null && order.getBonusUsed() > 0) {
            sb.append("🎁 Списано бонусов: -").append(order.getBonusUsed()).append(" ₽\n");
        }

        return sb.toString();
    }

    private String getPaymentMethodText(Order.PaymentMethod method) {
        switch (method) {
            case CASH: return "Наличными";
            case CARD_ONLINE: return "Картой онлайн";
            case CARD_ON_DELIVERY: return "Картой при получении";
            default: return "Не указано";
        }
    }

    /**
     * Отправка поздравительного письма на день рождения
     */
    public void sendBirthdayGreeting(User user, int bonusAmount) {
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            log.warn("У пользователя {} нет email", user.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail);
            message.setTo(email);
            message.setSubject("🎂 С Днём рождения от RedRolls! 🎉");
            message.setText(formatBirthdayMessage(user, bonusAmount));

            mailSender.send(message);
            log.info("Поздравительное письмо отправлено на {}", email);

        } catch (Exception e) {
            log.error("Ошибка отправки поздравительного письма для {}: {}", email, e.getMessage());
        }
    }

    /**
     * Форматирование поздравительного письма
     */
    private String formatBirthdayMessage(User user, int bonusAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎂🎉✨ ДОРОГОЙ(АЯ) ").append(user.getFirstName().toUpperCase()).append("! ✨🎉🎂\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("      С ДНЁМ РОЖДЕНИЯ! 🎈🎁🎊\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("Команда RedRolls поздравляет вас с этим чудесным днём!\n");
        sb.append("Желаем здоровья, счастья и конечно же вкусных роллов! 🍣\n\n");

        sb.append("🎁 ВАШ ПОДАРОК:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("✨ На ваш счёт начислено %d бонусов!\n", bonusAmount));
        sb.append("💡 1 бонус = 1 рубль\n");
        sb.append("💰 Бонусы можно использовать для оплаты до 30% от суммы заказа\n\n");

        sb.append("🍣 КАК ПОЛУЧИТЬ ПОДАРОК?\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("1. Выберите любимые роллы в нашем меню\n");
        sb.append("2. Оформите заказ с доставкой или самовывозом\n");
        sb.append("3. Примените бонусы при оплате\n\n");

        sb.append("🔗 Перейти в меню:\n");
        sb.append("https://redrolls.ru/\n\n");

        sb.append("🎂 Пусть этот день будет таким же ярким и вкусным,\n");
        sb.append("как наши роллы! 🍣\n\n");

        sb.append("С любовью,\n");
        sb.append("Команда RedRolls 💙");

        return sb.toString();
    }

}
