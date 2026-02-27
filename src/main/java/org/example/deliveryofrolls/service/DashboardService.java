package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.DashboardStats;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.repository.CategoryRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.example.deliveryofrolls.repository.OrderRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusWeeks(1).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusMonths(1).atStartOfDay();

        // Заказы
        stats.setTotalOrders(orderRepository.count()); // общее кол-во
        stats.setTodayOrders(orderRepository.countByCreatedAtAfter(startOfDay)); // кол-во за сегодня
        stats.setPendingOrders(orderRepository.countByStatus(Order.OrderStatus.PENDING)); // кол-во ожидающих подтверждения
        stats.setDeliveredOrders(orderRepository.countByStatus(Order.OrderStatus.DELIVERED)); // кол-во доставленных

        // Выручка
        stats.setTodayRevenue(orderRepository.sumTotalPriceByCreatedAtAfter(startOfDay));  // за день
        stats.setWeekRevenue(orderRepository.sumTotalPriceByCreatedAtAfter(startOfWeek)); // за неделю
        stats.setMonthRevenue(orderRepository.sumTotalPriceByCreatedAtAfter(startOfMonth)); // за месяц
        stats.setTotalRevenue(orderRepository.sumTotalPrice()); // за все время

        // Блюда
        stats.setTotalDishes(dishRepository.count());                     // всего
        stats.setAvailableDishes(dishRepository.countByAvailableTrue());  // доступных в меню
        stats.setPopularDishes(orderRepository.findTopPopularDishes());   // популярные

        // Пользователи
        stats.setTotalUsers(userRepository.count());   // общее кол-во
        stats.setNewTodayUsers(userRepository.countByRegisteredAtAfter(startOfDay)); // новых сегодня

        // Категории
        stats.setTotalCategories(categoryRepository.count()); // всего категорий

        return stats;
    }

}
