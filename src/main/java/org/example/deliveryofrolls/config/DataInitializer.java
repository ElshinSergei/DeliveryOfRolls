package org.example.deliveryofrolls.config;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createAdminUser();
        createTestUser();
        createTestOrders();
    }

    private void createAdminUser() {

        String adminEmail = "admin@redrolls.ru";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("Admin");
            admin.setPhone("+7 (999) 999-99-99");
            admin.setRole(User.Role.ROLE_ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);

        } else {
            System.out.println("Администратор уже существует");
        }
    }

    private void createTestUser() {
        String userEmail = "user@example.com";

        if (userRepository.findByEmail(userEmail).isEmpty()) {
            User user = new User();
            user.setEmail(userEmail);
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFirstName("Тестовый");
            user.setLastName("Пользователь");
            user.setPhone("+7 (999) 123-45-67");
            user.setRole(User.Role.ROLE_USER);
            user.setEnabled(true);
            user.setRegisteredAt(LocalDateTime.now());

            userRepository.save(user);
            System.out.println("✅ Тестовый пользователь создан");
        } else {
            System.out.println("ℹ️ Тестовый пользователь уже существует");
        }
    }

    private void createTestOrders() {
        User user = userRepository.findByEmail("user@example.com")
                .orElse(null);

        if (user == null) return;

        // Проверяем, есть ли уже заказы
        if (orderRepository.count() > 55) return;

        List<Order> orders = new ArrayList<>();
        Random random = new Random();

        for (int i = 1; i <= 100; i++) {
            Order order = new Order();
            order.setUser(user);
            order.setCustomerName("Тестовый Клиент " + i);
            order.setCustomerPhone("+7 (999) 123-45-67");
            order.setDeliveryAddress("ул. Тестовая, д. " + i);

            // Тип доставки
            order.setDeliveryType(i % 2 == 0 ?
                    Order.DeliveryType.DELIVERY : Order.DeliveryType.PICKUP);

            // Способ оплаты
            int payType = i % 3;
            if (payType == 0) order.setPaymentMethod(Order.PaymentMethod.CASH);
            else if (payType == 1) order.setPaymentMethod(Order.PaymentMethod.CARD_ONLINE);
            else order.setPaymentMethod(Order.PaymentMethod.CARD_ON_DELIVERY);

            // Статус
            int statusType = i % 8;
            switch (statusType) {
                case 0: order.setStatus(Order.OrderStatus.PENDING); break;
                case 1: order.setStatus(Order.OrderStatus.CONFIRMED); break;
                case 2: order.setStatus(Order.OrderStatus.PREPARING); break;
                case 3: order.setStatus(Order.OrderStatus.READY_FOR_DELIVERY); break;
                case 4: order.setStatus(Order.OrderStatus.ON_THE_WAY); break;
                case 5: order.setStatus(Order.OrderStatus.DELIVERED); break;
                case 6: order.setStatus(Order.OrderStatus.COMPLETED); break;
                case 7: order.setStatus(Order.OrderStatus.CANCELLED); break;
            }

            order.setTotalPrice(BigDecimal.valueOf(1000 + (i * 100)));
            order.setNotes("Тестовый комментарий " + i);

            // Даты с разбросом
            order.setCreatedAt(LocalDateTime.now().minusDays(i));
            order.setUpdatedAt(LocalDateTime.now().minusDays(i));
            order.setDeliveryTime(LocalDateTime.now().plusHours(2));

            orders.add(order);
        }

        orderRepository.saveAll(orders);
        System.out.println("✅ Добавлено 50 тестовых заказов");
    }

}
