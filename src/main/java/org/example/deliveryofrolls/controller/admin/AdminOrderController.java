package org.example.deliveryofrolls.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.OrderListDTO;
import org.example.deliveryofrolls.entity.Order;
import org.example.deliveryofrolls.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // ПОСМОТРЕТЬ ВСЕ ЗАКАЗЫ
    @GetMapping
    public String listOrders(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                 Pageable pageable,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                 LocalDate dateFrom,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                 LocalDate dateTo,
                             Model model,
                             HttpServletRequest request) {

        Page<OrderListDTO> orders = orderService.findOrdersByFilters(
                status, search, dateFrom, dateTo, pageable);

        // Получаем параметры сортировки из Pageable
        String currentSortField = "";
        String currentSortDir = "desc";
        String reverseSortDir = "asc";

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            currentSortField = order.getProperty();
            currentSortDir = order.getDirection().name().toLowerCase();
            reverseSortDir = currentSortDir.equals("asc") ? "desc" : "asc";
        } else {
            currentSortField = "createdAt";
        }

        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchQuery", search);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        model.addAttribute("currentSortField", currentSortField);
        model.addAttribute("currentSortDir", currentSortDir);
        model.addAttribute("reverseSortDir", reverseSortDir);

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", Order.OrderStatus.values());
        model.addAttribute("pageTitle", "Управление заказами");
        model.addAttribute("baseUrl", request.getRequestURI()); // Текущий URL
        return "admin/orders/list";
    }

    // ДЕТАЛЬНЫЙ ПРОСМОТР ЗАКАЗА
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId,
                               Model model) {
        // Получаем заказ с товарами
        Order order = orderService.getOrderWithItems(orderId);

        model.addAttribute("order", order);
        model.addAttribute("statuses", Order.OrderStatus.values());
        model.addAttribute("pageTitle", "Заказ #" + orderId);
        model.addAttribute("pageCss", "order-details.css");

        return "admin/orders/details";
    }

    //  ИЗМЕНЕНИЕ СТАТУСА ЗАКАЗА
    @PostMapping("/{orderId}/status")
    @ResponseBody
    public Map<String, Object> updateStatus(@PathVariable Long orderId,
                                            @RequestParam Order.OrderStatus status) {

        System.out.println("=== ВЫЗОВ МЕТОДА updateStatus ===");
        System.out.println("orderId: " + orderId);
        System.out.println("status: " + status);

        Map<String, Object> response = new HashMap<>();
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, status);
            response.put("success", true);
            response.put("message", "Статус заказа #" + orderId + " изменен на " + status);
            response.put("newStatus", status.name());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }
        return response;
    }

}
