package org.example.deliveryofrolls.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.OrderDTO;
import org.example.deliveryofrolls.dto.OrderListDTO;
import org.example.deliveryofrolls.dto.PromoCodeResponse;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.repository.OrderItemRepository;
import org.example.deliveryofrolls.repository.OrderRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final DashboardService dashboardService;
    private final PromoCodeService promoCodeService;
    private final EmailService emailService;
    private final BonusService bonusService;
    private final UserService userService;
    private final BonusSettingsService bonusSettingsService;

    // Получить заказ по ID (с кэшем)
    @Cacheable(value = "orders", key = "'user-' + #userId")
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Получить данные по заказу
    @Cacheable(value = "orders", key = "#orderId")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));
    }

    // Создание заказа
    @CacheEvict(value = "orders", allEntries = true)
    public Order createOrder(OrderDTO orderDTO, HttpSession session, UserDetails userDetails) {
        log.info("========== НАЧАЛО СОЗДАНИЯ ЗАКАЗА ==========");
        log.info("Session ID: {}, User: {}", session.getId(), userDetails != null ? userDetails.getUsername() : "guest");

        // 1. Получаем корзину
        Cart cart = cartService.getOrCreateCart(session, userDetails);
        log.info("1. Корзина: ID={}, всего товаров={}", cart.getId(), cart.getItems().size());

        if (cart.getItems().isEmpty()) {
            log.error("2. КОРЗИНА ПУСТА! Заказ не может быть создан");
            throw new IllegalStateException("Корзина пуста");
        }
        log.info("2. Корзина не пуста, продолжаем...");

        // 2. Конвертируем DTO в Order
        log.info("3. Конвертируем DTO в Order");
        Order order = orderDTO.toOrder();
        order.setDeliveryEntrance(orderDTO.getDeliveryEntrance());
        order.setDeliveryFloor(orderDTO.getDeliveryFloor());
        order.setDeliveryApartment(orderDTO.getDeliveryApartment());
        order.setDeliveryIntercom(orderDTO.getDeliveryIntercom());
        log.info("   Order после конвертации: customerName={}, phone={}, deliveryType={}, paymentMethod={}",
                order.getCustomerName(), order.getCustomerPhone(), order.getDeliveryType(), order.getPaymentMethod());

        // ===== ПЕРЕНОСИМ ИНФОРМАЦИЮ ИЗ КОРЗИНЫ В ЗАКАЗ =====
        if (cart.getDeliveryType() != null) {
            // Преобразуем строку в верхний регистр для enum
            String deliveryTypeStr = cart.getDeliveryType().toUpperCase();
            try {
                order.setDeliveryType(Order.DeliveryType.valueOf(deliveryTypeStr));
                log.info("Установлен тип доставки из корзины: {}", deliveryTypeStr);
            } catch (IllegalArgumentException e) {
                log.error("Неизвестный тип доставки: {}, используем DELIVERY по умолчанию", cart.getDeliveryType());
                order.setDeliveryType(Order.DeliveryType.DELIVERY);
                deliveryTypeStr = "DELIVERY";
            }

            if ("DELIVERY".equals(deliveryTypeStr)) {
                // Для доставки - берем адрес из корзины
                order.setDeliveryAddress(cart.getDeliveryAddress());
                log.info("   Адрес доставки из корзины: {}", cart.getDeliveryAddress());

                // Добавляем информацию о зоне в заметки (опционально)
                if (cart.getSelectedZoneName() != null) {
                    String zoneInfo = "Зона доставки: " + cart.getSelectedZoneName();
                    if (cart.getDeliveryTime() != null) {
                        zoneInfo += " (время: " + cart.getDeliveryTime() + ")";
                    }
                    order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + zoneInfo : zoneInfo);
                    log.info("   Добавлена информация о зоне: {}", zoneInfo);
                }

                // Проверяем минимальную сумму заказа
                if (cart.getMinOrderRequired() != null &&
                        cart.getTotalPrice().compareTo(BigDecimal.valueOf(cart.getMinOrderRequired())) < 0) {
                    String errorMsg = "Минимальная сумма заказа для зоны \"" + cart.getSelectedZoneName() + "\": " +
                            cart.getMinOrderRequired() + " ₽. Добавьте товаров на " +
                            (cart.getMinOrderRequired() - cart.getTotalPrice().intValue()) + " ₽";
                    log.error("   {}", errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
                log.info("   Минимальная сумма заказа проверена: {} >= {}",
                        cart.getTotalPrice(), cart.getMinOrderRequired());

            } else if ("PICKUP".equals(deliveryTypeStr)) {
                // Для самовывоза - адрес не нужен
                order.setDeliveryAddress(null);
                log.info("   Самовывоз: адрес не требуется");

                if (cart.getPickupPointName() != null) {
                    String pickupInfo = "Самовывоз из: " + cart.getPickupPointName() + "\nАдрес: " + cart.getDeliveryAddress();
                    order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + pickupInfo : pickupInfo);
                    log.info("   Добавлена информация о точке самовывоза: {}", pickupInfo);
                }
            }
        } else {
            log.info("   В корзине нет информации о типе доставки, используем данные из DTO");
        }

        // 3. Устанавливаем пользователя
        if (userDetails != null) {
            log.info("4. Ищем пользователя: {}", userDetails.getUsername());
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            order.setUser(user);
            log.info("   Пользователь найден: ID={}", user.getId());
        }

        // 4. Получаем сумму корзины
        BigDecimal cartTotal = cart.getTotalPrice();
        String promoCode = orderDTO.getAppliedPromoCode();
        boolean hasPromoCode = promoCode != null && !promoCode.isEmpty();
        boolean hasBonuses = userDetails != null && orderDTO.getUsedBonuses() != null && orderDTO.getUsedBonuses() > 0;

        // ===== ПРОВЕРКА НА ОДНОВРЕМЕННОЕ ИСПОЛЬЗОВАНИЕ =====
        if (hasPromoCode && hasBonuses) {
            throw new IllegalArgumentException("Нельзя одновременно использовать промокод и бонусы");
        }
        // ===================================================

        BigDecimal finalPrice = cartTotal;

        // 5. Применяем бонусы (только если нет промокода)
        if (hasBonuses) {
            log.info("5. Обнаружено использование бонусов: {}", orderDTO.getUsedBonuses());

            try {
                User user = userService.getCurrentUser(userDetails);
                int usedBonuses = orderDTO.getUsedBonuses();

                // ===== НОВАЯ ПРОВЕРКА МИНИМАЛЬНОЙ СУММЫ =====
                BonusSettings settings = bonusSettingsService.getSettings();
                if (cartTotal.compareTo(settings.getMinOrderAmount()) < 0) {
                    throw new IllegalArgumentException(
                            "Минимальная сумма заказа для списания бонусов: " +
                                    settings.getMinOrderAmount() + " ₽"
                    );
                }
                // ============================================

                int maxSpendable = bonusService.getMaxSpendableForCart(cartTotal, user);

                if (usedBonuses > maxSpendable) {
                    throw new IllegalArgumentException("Нельзя использовать больше " + maxSpendable + " бонусов");
                }

                BonusAccount account = bonusService.getOrCreateAccount(user);
                if (usedBonuses > account.getBalance()) {
                    throw new IllegalArgumentException("Недостаточно бонусов на счете");
                }

                boolean spent = bonusService.spendBonusForOrder(user, usedBonuses);
                if (spent) {
                    finalPrice = finalPrice.subtract(BigDecimal.valueOf(usedBonuses));
                    order.setBonusUsed(usedBonuses);
                    log.info("   ✅ Списано {} бонусов для заказа", usedBonuses);
                }
            } catch (Exception e) {
                log.error("   Ошибка при списании бонусов: {}", e.getMessage());
                throw new IllegalArgumentException("Ошибка при списании бонусов: " + e.getMessage());
            }
        }

        // 6. Применяем промокод (только если нет бонусов)
        if (hasPromoCode) {
            log.info("6. Обнаружен промокод: {}", promoCode);

            try {
                // Проверяем промокод через сервис
                PromoCodeResponse promoResponse = promoCodeService.applyPromoCode(promoCode, cartTotal);

                if (promoResponse.isValid()) {
                    // Промокод действителен - применяем скидку
                    finalPrice = promoResponse.getFinalAmount();

                    // Сохраняем информацию о промокоде в заказ
                    order.setPromoCode(promoCode);
                    order.setDiscountAmount(promoResponse.getDiscountAmount());

                    // Отмечаем промокод как использованный
                    promoCodeService.usePromoCode(promoCode);

                    log.info("   ✅ Промокод применен: скидка {} ₽, итог {} ₽",
                            promoResponse.getDiscountAmount(), finalPrice);
                } else {
                    // Промокод недействителен - логируем причину, но заказ создаем без скидки
                    log.warn("   ❌ Промокод не применен: {}", promoResponse.getMessage());
                    order.setPromoCode(promoCode);
                    order.setDiscountAmount(BigDecimal.ZERO);
                }
            } catch (Exception e) {
                // Ошибка при проверке промокода - создаем заказ без скидки
                log.error("   Ошибка при проверке промокода: {}", e.getMessage());
                order.setPromoCode(promoCode);
                order.setDiscountAmount(BigDecimal.ZERO);
            }
        } else if (!hasBonuses) {
            log.info("6. Промокод не указан");
        }

        // 7. Устанавливаем итоговую сумму
        order.setTotalPrice(finalPrice);
        log.info("7. Итоговая сумма заказа: {} ₽", finalPrice);

        // 8. Сохраняем заказ
        log.info("8. Сохраняем заказ в БД");
        Order savedOrder = orderRepository.save(order);
        log.info("   Заказ сохранен: ID={}", savedOrder.getId());

        // 9. Создаем OrderItem из CartItem
        log.info("9. Создаем OrderItem из CartItem");
        int itemCount = 0;
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
            itemCount++;
            log.info("   Создан OrderItem: {} x {} = {}", dish.getName(), cartItem.getQuantity(), orderItem.getTotalPrice());
        }
        log.info("   Всего создано OrderItem: {}", itemCount);

        // 10. Получаем свежую версию заказа с товарами
        log.info("10. Загружаем свежую версию заказа с товарами");
        Order freshOrder = orderRepository.findByIdWithItems(savedOrder.getId())
                .orElse(savedOrder);
        log.info("   freshOrder ID: {}, items size: {}", freshOrder.getId(), freshOrder.getItems().size());

        // Если размер 0, пробуем загрузить отдельно
        if (freshOrder.getItems().isEmpty()) {
            List<OrderItem> items = orderItemRepository.findByOrderId(savedOrder.getId());
            log.info("   Найдено OrderItem через репозиторий: {}", items.size());

            // НЕ заменяем коллекцию, а очищаем и добавляем
            freshOrder.getItems().clear();
            freshOrder.getItems().addAll(items);
        }

        // 11. Отправляем уведомления
        log.info("11. Отправляем уведомления");
        emailService.sendOrderConfirmationToCustomer(freshOrder);
        emailService.sendOrderNotificationToKitchen(freshOrder);

        // 12. Начисляем бонусы за заказ (только если не использовались бонусы при оплате)
        if (userDetails != null && !hasBonuses) {
            int bonusEarned = bonusService.calculateEarnedBonuses(finalPrice);
            savedOrder.setBonusEarned(bonusEarned);
            bonusService.earnBonusForOrder(savedOrder);
            log.info("   Начислено {} бонусов за заказ", bonusEarned);
        }

        // 13. Очищаем корзину
        log.info("13. Очищаем корзину");
        if (userDetails != null) {
            cartService.clearCart(cart.getId(), session);
            log.info("   Корзина пользователя очищена");
        } else {
            cartService.clearCart(session.getId());
            log.info("   Корзина гостя очищена");
        }

        log.info("✅ ЗАКАЗ #{} УСПЕШНО СОЗДАН", savedOrder.getId());

        // Логируем информацию о промокоде и бонусах
        if (savedOrder.getPromoCode() != null) {
            log.info("   Промокод: {}, скидка: {} ₽",
                    savedOrder.getPromoCode(), savedOrder.getDiscountAmount());
        }
        if (savedOrder.getBonusUsed() != null && savedOrder.getBonusUsed() > 0) {
            log.info("   Использовано бонусов: {} ₽", savedOrder.getBonusUsed());
        }
        if (savedOrder.getBonusEarned() != null && savedOrder.getBonusEarned() > 0) {
            log.info("   Начислено бонусов: {}", savedOrder.getBonusEarned());
        }

        log.info("========== КОНЕЦ СОЗДАНИЯ ЗАКАЗА ==========");

        dashboardService.evictCache();

        return savedOrder;
    }

    // Обновление статуса заказа
    @CacheEvict(value = "orders", allEntries = true)
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = getOrder(orderId);
        Order.OrderStatus oldStatus = order.getStatus();
        // Если заказ отменяют
        if (newStatus == Order.OrderStatus.CANCELLED && oldStatus != Order.OrderStatus.CANCELLED) {
            log.info("Заказ #{} отменяется. Статус изменен с {} на {}", orderId, oldStatus, newStatus);
            // 1. Возвращаем списанные бонусы (если были)
            if (order.getBonusUsed() != null && order.getBonusUsed() > 0) {
                bonusService.refundBonuses(order);
                log.info("Возвращено {} списанных бонусов", order.getBonusUsed());
            }
            // 2. СПИСЫВАЕМ начисленные бонусы (если были)
            if (order.getBonusEarned() != null && order.getBonusEarned() > 0) {
                bonusService.deductEarnedBonuses(order);
                log.info("Списано {} начисленных бонусов", order.getBonusEarned());
            }
        }
        order.setStatus(newStatus);
        log.info("📝 Статус заказа #{} изменен с {} на {}",
                orderId, oldStatus, newStatus);
        return orderRepository.save(order);
    }

    // Получить заказ с товарами для детального просмотра
    @Cacheable(value = "orders", key = "'items-' + #orderId")
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

        log.info("🔄 Повтор заказа #{}. Добавлено: {}, пропущено: {}", orderId, added, skipped);
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

    public long countNewOrders() {
        return orderRepository.countByStatus(Order.OrderStatus.PENDING);
    }
}
