package org.example.deliveryofrolls.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.CartItem;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.CartItemRepository;
import org.example.deliveryofrolls.repository.CartRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final CartItemRepository cartItemRepository;

    // Получить или создать корзину для пользователя
    public Cart getOrCreateCart(HttpSession session, UserDetails userDetails) {
        if (userDetails != null) {
            return getOrCreateCartForUser(userDetails.getUsername());
        } else {
            return getOrCreateCartForSession(session);
        }
    }

    // Для авторизованных пользователей
    public Cart getOrCreateCartForUser(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setSessionId(null);
                    return cartRepository.save(cart);
                });
    }

    public Cart getOrCreateCartForSession(HttpSession session) {
        String sessionId = session.getId();

        Optional<Cart> cartFromDb = cartRepository.findBySessionId(sessionId);

        if (cartFromDb.isPresent()) {
            return cartFromDb.get();
        }

        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setUser(null);
        Cart savedCart = cartRepository.save(cart);
        session.setAttribute("cart", savedCart);
        return savedCart;
    }

    // Добавление товара в корзину
    public void addToCart(HttpSession session, UserDetails userDetails, Long dishId, int quantity) {
        Cart cart = getOrCreateCart(session, userDetails);
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getDish().getId().equals(dishId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setDish(dish);
            cartItem.setQuantity(quantity);
            cartItem.setPriceAtTime(dish.getPrice());
            cart.getItems().add(cartItem);
        }

        cartRepository.save(cart);

        session.setAttribute("cart", cart);
    }

    // Объединение корзин при авторизации
    @Transactional
    public void mergeCarts(HttpSession session, UserDetails userDetails) {
        if (userDetails == null) return;

        String sessionId = session.getId();
        log.info("========== ДИАГНОСТИКА MERGE ==========");
        log.info("1. Session ID: {}", sessionId);

        // 1. Ищем в БД
        Optional<Cart> sessionCartFromDb = cartRepository.findBySessionIdWithItems(sessionId);
        Cart anonymousCart = null;

        if (sessionCartFromDb.isPresent()) {
            // Есть в БД - используем её
            anonymousCart = sessionCartFromDb.get();
            log.info("2. Корзина в БД: ID={}, товаров={}",
                    anonymousCart.getId(), anonymousCart.getItems().size());
        } else {
            // Нет в БД - проверяем сессию
            Cart sessionCart = (Cart) session.getAttribute("cart");
            log.info("2. Корзина в БД не найдена, в сессии: {}",
                    sessionCart != null ? "ID=" + sessionCart.getId() : "null");

            if (sessionCart != null) {
                // Сохраняем сессионную корзину в БД
                log.info("3. Сохраняем сессионную корзину в БД");
                anonymousCart = cartRepository.save(sessionCart);

                // Перезагружаем с товарами
                anonymousCart = cartRepository.findBySessionIdWithItems(sessionId)
                        .orElse(anonymousCart);
            }
        }

        if (anonymousCart == null) {
            log.info("4. Анонимная корзина не найдена");
            return;
        }

        int itemsCount = anonymousCart.getItems().size();
        log.info("4. Анонимная корзина: ID={}, товаров={}",
                anonymousCart.getId(), itemsCount);

        if (itemsCount == 0) {
            log.info("5. Корзина пуста, выход");
            return;
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        log.info("6. Пользователь: ID={}, email={}", user.getId(), user.getEmail());

        // Получаем корзину пользователя
        Cart userCart = getOrCreateCartForUser(userDetails.getUsername());
        log.info("7. Корзина пользователя: ID={}, товаров ДО={}",
                userCart.getId(), userCart.getItems().size());

        // Переносим товары
        int moved = 0;
        for (CartItem anonymousItem : new ArrayList<>(anonymousCart.getItems())) {
            log.info("   Переносим товар: dishId={}, quantity={}",
                    anonymousItem.getDish().getId(), anonymousItem.getQuantity());

            // Ищем такой же товар в корзине пользователя
            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getDish().getId().equals(anonymousItem.getDish().getId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(
                        existingItem.get().getQuantity() + anonymousItem.getQuantity()
                );
                log.info("   → Товар уже был, увеличили количество до {}",
                        existingItem.get().getQuantity());

                // Удаляем старый товар из анонимной корзины
                cartItemRepository.delete(anonymousItem);
            } else {
                // Перепривязываем к корзине пользователя
                anonymousItem.setCart(userCart);
                userCart.getItems().add(anonymousItem);
                log.info("   → Добавлен новый товар в корзину пользователя");
            }
            moved++;
        }

        // Сохраняем корзину пользователя
        cartRepository.save(userCart);
        cartRepository.flush();
        log.info("8. Корзина пользователя сохранена. Товаров ПОСЛЕ={}",
                userCart.getItems().size());

        // Удаляем анонимную корзину
        if (anonymousCart.getId() != null && !anonymousCart.getId().equals(userCart.getId())) {
            log.info("9. Удаляем анонимную корзину ID={}", anonymousCart.getId());
            cartRepository.delete(anonymousCart);
        }

        // Обновляем сессию
        session.removeAttribute("cart");
        session.setAttribute("cart", userCart);
        log.info("10. Сессия обновлена, установлена корзина ID={}", userCart.getId());

        log.info("========== MERGE ЗАВЕРШЕН, перенесено товаров: {} ==========", moved);
    }

    // Очистка корзины по ID
    public void clearCart(Long cartId, HttpSession session) {
        log.info("===== НАЧАЛО ОЧИСТКИ КОРЗИНЫ =====");
        log.info("ID корзины для очистки: {}", cartId);

        Optional<Cart> cartOpt = cartRepository.findById(cartId);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            log.info("Корзина найдена. Товаров ДО очистки: {}", cart.getItems().size());

            int itemsCount = cart.getItems().size();

            if (itemsCount > 0) {
                List<Long> itemIds = cart.getItems().stream()
                        .map(CartItem::getId)
                        .collect(Collectors.toList());
                log.info("ID товаров для удаления: {}", itemIds);

                // Удаляем
                cart.getItems().clear();
                cartRepository.save(cart);
                cartRepository.flush();

                log.info("Удалено товаров: {}", itemsCount);
            } else {
                log.info("Корзина уже пуста");
            }

            // Проверяем после очистки
            Cart afterClear = cartRepository.findById(cartId).orElse(null);
            if (afterClear != null) {
                log.info("Товаров ПОСЛЕ очистки: {}", afterClear.getItems().size());
            }

            session.removeAttribute("cart");

            log.info("===== КОНЕЦ ОЧИСТКИ КОРЗИНЫ ===== удалено товаров: {}", itemsCount);
        } else {
            log.error("Корзина с ID {} не найдена!", cartId);
        }
    }

    // Очистка корзины по sessionId
    public void clearCart(String sessionId) {
        Optional<Cart> cartOpt = cartRepository.findBySessionId(sessionId);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();

            // Получаем список ID товаров ДО очистки
            List<Long> itemIds = cart.getItems().stream()
                    .map(CartItem::getId)
                    .collect(Collectors.toList());
            // Удаляем все товары по ID
            if (!itemIds.isEmpty()) {
                cartItemRepository.deleteAllById(itemIds);
                log.info("Удалено товаров из БД: {}", itemIds.size());
            }
            // Очищаем список в корзине
            cart.getItems().clear();
            // Сохраняем корзину
            cartRepository.save(cart);

            log.info("Корзина {} полностью очищена. Удалено товаров: {}", sessionId, itemIds.size());
        }
    }

    public void removeItemFromCart(Long itemId, HttpSession session) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Элемент корзины не найден"));

        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cartRepository.save(cart);
        session.setAttribute("cart", cart);
    }

    // Увеличить элемент корзины на 1
    public void increaseQuantity(Long itemId, Integer increment, HttpSession session) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        int newQuantity = cartItem.getQuantity() + increment;

        if (newQuantity > 99) {
            throw new IllegalArgumentException("Максимальное количество - 99");
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        session.setAttribute("cart", cartItem.getCart());
    }

    // Уменьшить элемент корзины на 1
    public void decreaseQuantity(Long itemId, Integer decrement, HttpSession session) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        int newQuantity = cartItem.getQuantity() - decrement;

        if (newQuantity <= 0) {
            removeItemFromCart(itemId, session);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
            session.setAttribute("cart", cartItem.getCart());
        }
    }

    // УМЕНЬШАЕМ КОЛ-ВО ПО dishId
    public void decreaseFromCart(HttpSession session, UserDetails userDetails, Long dishId) {
        Cart cart = getOrCreateCart(session, userDetails);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getDish().getId().equals(dishId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        if (item.getQuantity() <= 1) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(item.getQuantity() - 1);
            cartItemRepository.save(item);
            session.setAttribute("cart", cart);
        }
        cartRepository.save(cart);
        session.setAttribute("cart", cart);

    }

    public void save(Cart cart) {
        cartRepository.save(cart);
        log.info("Корзина сохранена: ID={}, deliveryType={}", cart.getId(), cart.getDeliveryType());
    }
}
