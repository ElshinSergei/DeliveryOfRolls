package org.example.deliveryofrolls.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.CartItem;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.CartItemRepository;
import org.example.deliveryofrolls.repository.CartRepository;
import org.example.deliveryofrolls.repository.DishRepository;
import org.example.deliveryofrolls.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor  // Lombok генерирует конструктор
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final CartItemRepository cartItemRepository;

    // Получить или создать корзину для пользователя
    public Cart getOrCreateCart(HttpSession session, UserDetails userDetails) {

        if (userDetails != null) {
            // Пользователь авторизован - работаем с БД
            return getOrCreateCartForUser(userDetails.getUsername());
        } else {
            // Пользователь анонимный - работаем с сессией
            return getOrCreateCartForSession(session);
        }
    }

    // Для авторизованных пользователей (БД)
    private Cart getOrCreateCartForUser(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setSessionId(null); // У авторизованных нет sessionId
                    return cartRepository.save(cart);
                });
    }

    // Для анонимных пользователей (сессия)
    private Cart getOrCreateCartForSession(HttpSession session) {
        String sessionId = session.getId();

        // Пытаемся найти корзину в БД по sessionId
        Optional<Cart> cartFromDb = cartRepository.findBySessionId(sessionId);

        if (cartFromDb.isPresent()) {
            return cartFromDb.get();
        }
        // Если нет в БД, создаем новую
        Cart cart = new Cart();
        cart.setSessionId(sessionId);
        cart.setUser(null); // Анонимный пользователь

        Cart savedCart = cartRepository.save(cart);

        // Сохраняем также в сессии для быстрого доступа
        session.setAttribute("cart", savedCart);

        return savedCart;
    }

    // Добавление товара в корзину
    public void addToCart(HttpSession session, UserDetails userDetails, Long dishId, int quantity) {
        Cart cart = getOrCreateCart(session, userDetails);
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new IllegalArgumentException("Блюдо не найдено"));

        // Проверяем, есть ли уже это блюдо в корзине
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getDish().getId().equals(dishId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Увеличиваем количество
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            // Создаем новый элемент корзины
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setDish(dish);
            cartItem.setQuantity(quantity);
            cartItem.setPriceAtTime(dish.getPrice()); // Сохраняем цену на момент добавления

            cart.getItems().add(cartItem);
        }

        cartRepository.save(cart);

        // Обновляем в сессии (если анонимный)
        if (userDetails == null) {
            session.setAttribute("cart", cart);
        }
    }

    // Объединение корзин при авторизации
    public void mergeCarts(HttpSession session, UserDetails userDetails) {
        if (userDetails == null) {
            return;
        }
        // 1. Получаем анонимную корзину из сессии
        Cart anonymousCart = (Cart) session.getAttribute("cart");

        if (anonymousCart == null || anonymousCart.getItems().isEmpty()) {
            return; // Нет товаров в анонимной корзине
        }

        // 2. Получаем корзину пользователя
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Cart userCart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        // 3. Переносим товары из анонимной в пользовательскую корзину
        for (CartItem anonymousItem : anonymousCart.getItems()) {
            boolean exists = userCart.getItems().stream()
                    .anyMatch(item -> item.getDish().getId().equals(anonymousItem.getDish().getId()));

            if (exists) {
                // Увеличиваем количество
                userCart.getItems().stream()
                        .filter(item -> item.getDish().getId().equals(anonymousItem.getDish().getId()))
                        .findFirst()
                        .ifPresent(item ->
                                item.setQuantity(item.getQuantity() + anonymousItem.getQuantity())
                        );
            } else {
                // Копируем элемент
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setDish(anonymousItem.getDish());
                newItem.setQuantity(anonymousItem.getQuantity());
                newItem.setPriceAtTime(anonymousItem.getPriceAtTime());
                newItem.setSpecialInstructions(anonymousItem.getSpecialInstructions());

                userCart.getItems().add(newItem);
            }
        }

        // 4. Сохраняем и очищаем сессию
        cartRepository.save(userCart);
        session.removeAttribute("cart");

        // 5. Удаляем анонимную корзину из БД
        cartRepository.delete(anonymousCart);
    }

    public void clearCart(Long cartId) {
        Optional<Cart> cart = cartRepository.findById(cartId);
        if(cart.isPresent()) {
            cart.get().getItems().clear();
            cartRepository.save(cart.get());
        }

    }

    public void clearCart(String sessionId) {
        Optional<Cart> cart = cartRepository.findBySessionId(sessionId);
        if(cart.isPresent()) {
            cart.get().getItems().clear();
            cartRepository.save(cart.get());
        }
    }

    public void removeItemFromCart(Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Элемент корзины не найден"));

        // Получаем корзину и удаляем элемент из коллекции
        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);

        // Сохраняем изменения в корзине
        cartRepository.save(cart);
    }

    // Увеличить элемент корзины на 1
    public void increaseQuantity(Long itemId, Integer increment) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        int newQuantity = cartItem.getQuantity() + increment;

        // Проверка максимума
        if (newQuantity > 99) {
            throw new IllegalArgumentException("Максимальное количество - 99");
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
    }

    // Уменьшить элемент корзины на 1
    public void decreaseQuantity(Long itemId, Integer decrement) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден в корзине"));

        int newQuantity = cartItem.getQuantity() - decrement;

        if (newQuantity <= 0) {
            // Удаляем товар из корзины если количество стало 0
            removeItemFromCart(itemId);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }
    }


}
