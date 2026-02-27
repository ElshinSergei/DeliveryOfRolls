package org.example.deliveryofrolls.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {

    // Заказы
    private long totalOrders;      // общее кол-во
    private long todayOrders;      // кол-во за сегодня
    private long pendingOrders;    // кол-во ожидающих подтверждения
    private long deliveredOrders;  // кол-во доставленных

    // Выручка
    private BigDecimal todayRevenue; // за день
    private BigDecimal weekRevenue;  // за неделю
    private BigDecimal monthRevenue; // за месяц
    private BigDecimal totalRevenue; // за все время

    // Блюда
    private long totalDishes;  // всего блюд
    private long availableDishes; // доступных в меню
    private List<Map<String, Object>> popularDishes; // популярные

    // Пользователи
    private long totalUsers;     // всего
    private long newTodayUsers;  // новых сегодня

    // Категории
    private long totalCategories;  // всего категорий

}
