package org.example.deliveryofrolls.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.OrderDTO;
import org.example.deliveryofrolls.dto.OrderListDTO;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.repository.OrderItemRepository;
import org.example.deliveryofrolls.repository.OrderRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final UserRepository userRepository;

    // Создание заказа
    public Order createOrder(OrderDTO orderDTO, HttpSession session, UserDetails userDetails) {
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        Order order = orderDTO.toOrder();
        // Устанавливаем пользователя (если авторизован)
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            order.setUser(user);
        }
        // Устанавливаем сумму из корзины
        order.setTotalPrice(cart.getTotalPrice());

        Order savedOrder = orderRepository.save(order);

        // Создаем OrderItem из CartItem
        for (CartItem cartItem : cart.getItems()) {
            Dish dish = cartItem.getDish();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setDish(dish);
            orderItem.setDishName(dish.getName());
            orderItem.setPriceAtOrder(dish.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSpecialInstructions(cartItem.getSpecialInstructions());
            orderItem.calculateTotal();

            orderItemRepository.save(orderItem);
        }

        // Очищаем корзину
        if (userDetails != null) {
            // Авторизованный - очищаем по ID корзины
            cartService.clearCart(cart.getId());
        } else {
            // Гость - очищаем по sessionId
            cartService.clearCart(session.getId());
        }

        log.info("✅ Заказ #{} создан. Клиент: {}, Сумма: {} ₽",
                savedOrder.getId(), savedOrder.getCustomerName(), savedOrder.getTotalPrice());

        return savedOrder;
    }

    // Получить данные по заказу
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));
    }

    // Получить все заказы пользователя
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Получить все заказы
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    // Обновление статуса заказа
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = getOrder(orderId);
        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        log.info("📝 Статус заказа #{} изменен с {} на {}",
                orderId, oldStatus, newStatus);

        return orderRepository.save(order);
    }

    // Получить заказ с товарами для детального просмотра
    public Order getOrderWithItems(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));
    }

    // Повторить заказ
    public void repeatOrder(Long orderId, HttpSession session, UserDetails userDetails) {
        // Получаем заказ с товарами
        Order oldOrder = getOrderWithItems(orderId);

        if (oldOrder == null) {
            throw new IllegalArgumentException("Заказ не найден");
        }

        // Для авторизованных пользователей проверяем принадлежность
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            if (oldOrder.getUser() != null && !oldOrder.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Это не ваш заказ");
            }
        }

        Cart cart = cartService.getOrCreateCart(session, userDetails);

        int added = 0;
        int skipped = 0;

        // Добавляем товары
        for (OrderItem item : oldOrder.getItems()) {
            Dish dish = item.getDish();

            if (dish != null && dish.isAvailable()) {
                // Проверяем, есть ли уже такое блюдо в корзине
                cartService.addToCart(session, userDetails, dish.getId(), item.getQuantity());
                added++;
            } else {
                skipped++;
            }
        }

        if (added == 0) {
            throw new IllegalStateException("Ни одно блюдо из заказа сейчас не доступно");
        }
    }


    // Динамическая фильтрация
    public Page<OrderListDTO> findOrdersByFilters(String status, String search,
                                                  LocalDate dateFrom, LocalDate dateTo,
                                                  Pageable pageable) {

        // Спецификация для динамических фильтров
        Specification<Order> spec = Specification.where(null);
        // Фильтр по статусу
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), Order.OrderStatus.valueOf(status)));
        }
        // Поиск по имени или телефону
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("customerName")), searchPattern),
                            cb.like(root.get("customerPhone"), searchPattern)
                    ));
        }
        // Фильтр по дате начала
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
        }
        // Фильтр по дате окончания
        if (dateTo != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(23, 59, 59)));
        }
        return orderRepository.findAll(spec, pageable)
                .map(OrderListDTO::fromEntity);
    }
}
