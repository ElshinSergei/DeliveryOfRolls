package org.example.deliveryofrolls.config;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.OrderRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker;

    @Override
    public void run(String... args) throws Exception {
        createAdminUser();
        createTestUser();
    }

    private void createAdminUser() {

        String adminEmail = "admin@redrolls.ru";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("Admin");
            admin.setPhone("79999999999");
            admin.setRole(User.Role.ROLE_ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);

        } else {
            System.out.println("Администратор уже существует");
        }
    }

    private void createTestUser() {
        Random random = new Random();
        if(userRepository.count() < 50) {
            for (int i = 1; i <= 50; i++) {
                String email = faker.internet().emailAddress();

                if (userRepository.findByEmail(email).isPresent()) {
                    continue;
                }
                User user = new User();
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode("password123"));
                user.setFirstName(faker.name().firstName());
                user.setLastName(faker.name().lastName());
                user.setPhone(faker.phoneNumber().phoneNumber());
                user.setRole(User.Role.ROLE_USER);
                user.setEnabled(true);
                userRepository.save(user);

                // Создаем для пользователя 1-3 заказа
                int ordersCount = random.nextInt(3) + 1; // 1-3 заказа
                createOrdersForUser(user, ordersCount);
            }
            System.out.println("Готово! Создано 50 тестовых пользователей с заказами.");
        }
    }

    private void createOrdersForUser(User user, int orderCount) {
        Random random = new Random();

        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setUser(user);

            // Генерируем случайную сумму заказа (500-2000 ₽)
            BigDecimal totalPrice = BigDecimal.valueOf(500 + random.nextInt(1500));
            order.setTotalPrice(totalPrice);

            // Адрес доставки
            order.setDeliveryAddress(faker.address().streetAddress() + ", " +
                    faker.address().city());

            // Контактные данные
            order.setCustomerName(user.getFirstName() + " " + user.getLastName());
            order.setCustomerPhone(user.getPhone());

            // Тип доставки (80% доставка, 20% самовывоз)
            if (random.nextInt(100) < 80) {
                order.setDeliveryType(Order.DeliveryType.DELIVERY);
            } else {
                order.setDeliveryType(Order.DeliveryType.PICKUP);
            }

            // Способ оплаты
            int paymentRand = random.nextInt(100);
            if (paymentRand < 40) {
                order.setPaymentMethod(Order.PaymentMethod.CASH);
            } else if (paymentRand < 70) {
                order.setPaymentMethod(Order.PaymentMethod.CARD_ON_DELIVERY);
            } else {
                order.setPaymentMethod(Order.PaymentMethod.CARD_ONLINE);
            }

            // Случайный статус заказа
            Order.OrderStatus[] statuses = Order.OrderStatus.values();
            order.setStatus(statuses[random.nextInt(statuses.length)]);

            // Желаемое время доставки (случайное время в ближайшие 7 дней)
            order.setDeliveryTime(LocalDateTime.now()
                    .plusDays(random.nextInt(7))
                    .plusHours(random.nextInt(12))
                    .withMinute(0)
                    .withSecond(0));

            // Комментарий (с вероятностью 30%)
            if (random.nextInt(100) < 30) {
                String[] comments = {
                        "Позвонить за 10 минут",
                        "Домофон 123, 5 подъезд",
                        "Без лука пожалуйста",
                        "Оставить у двери",
                        "Осторожно, злая собака",
                        "Код домофона 456",
                        "Подарочная упаковка"
                };
                order.setNotes(comments[random.nextInt(comments.length)]);
            }

            // Даты создания (случайные за последние 30 дней)
            order.setCreatedAt(LocalDateTime.now()
                    .minusDays(random.nextInt(30))
                    .minusHours(random.nextInt(24)));

            orderRepository.save(order);
        }
    }
}
