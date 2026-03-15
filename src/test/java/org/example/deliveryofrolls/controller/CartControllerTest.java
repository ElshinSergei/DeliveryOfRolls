package org.example.deliveryofrolls.controller;

import org.example.deliveryofrolls.entity.Cart;
import org.example.deliveryofrolls.entity.CartItem;
import org.example.deliveryofrolls.entity.Dish;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(username = "test@test.com", roles = "USER")
@ActiveProfiles("test")
@Transactional
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private DishRepository dishRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    private Dish testDish;
    private User testUser;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testDish = new Dish();
        testDish.setName("Филадельфия");
        testDish.setPrice(new BigDecimal("500"));
        testDish.setAvailable(true);
        testDish.setWeight(250);
        testDish = dishRepository.save(testDish);

        testUser = new User();
        testUser.setEmail("test@test.com");
        testUser.setPassword("password");
        testUser.setFirstName("Тест");
        testUser.setLastName("Тестов");
        testUser.setPhone("+79991234567");
        testUser = userRepository.save(testUser);

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart = cartRepository.save(testCart);
        testUser.setCart(testCart);

        testCartItem = new CartItem();
        testCartItem.setCart(testCart);
        testCartItem.setDish(testDish);
        testCartItem.setQuantity(2);
        testCartItem.setPriceAtTime(testDish.getPrice());
        testCartItem = cartItemRepository.save(testCartItem);

        testCart.getItems().add(testCartItem);
        cartRepository.save(testCart);
    }

    @Test
    @Order(1)
    @DisplayName("1. Просмотр корзины")
    public void viewCart_returnsPage() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart"))
                .andExpect(model().attributeExists("cart"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Добавление нового товара в корзину")
    public void addToCart_newItem_success() throws Exception {
        mockMvc.perform(post("/cart/add/{dishId}", testDish.getId())
                        .with(csrf())
                        .param("quantity", "3")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.cartCount").value(5));
    }

    @Test
    @Order(3)
    @DisplayName("3. Добавление существующего товара - увеличение количества")
    public void addToCart_existingItem_increasesQuantity() throws Exception {
        mockMvc.perform(post("/cart/add/{dishId}", testDish.getId())
                        .with(csrf())
                        .param("quantity", "1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.cartCount").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("4. Добавление несуществующего товара - ошибка")
    @WithMockUser(username = "test@test.com")
    public void addToCart_nonExistingDish_error() throws Exception {
        mockMvc.perform(post("/cart/add/{dishId}", 9999L)
                        .with(csrf())
                        .param("quantity", "1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Уменьшение количества товара")
    public void decreaseFromCart_validItem_decreasesQuantity() throws Exception {
        mockMvc.perform(post("/cart/decrease-dish/{dishId}", testDish.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.cartCount").value(1));
    }

    @Test
    @Order(6)
    @DisplayName("6. Получение состояния корзины")
    public void getCartState_returnsCorrectState() throws Exception {
        mockMvc.perform(get("/cart/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("7. Удаление товара из корзины")
    public void removeItem_validId_redirects() throws Exception {
        mockMvc.perform(post("/cart/remove/{itemId}", testCartItem.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Увеличение количества товара")
    public void increaseQuantity_validItem_increases() throws Exception {
        mockMvc.perform(post("/cart/increase/{itemId}", testCartItem.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Очистка корзины")
    public void clearCart_withItems_clearsCart() throws Exception {
        mockMvc.perform(post("/cart/clear")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("successMessage"));
    }

}
