package org.example.deliveryofrolls.config;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.entity.*;
import org.example.deliveryofrolls.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final PromotionRepository promotionRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker;

    @Override
    public void run(String... args) throws Exception {
        createAdminUser();
        createCategories();
        createDishes();
        createPromotions();
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

    private void createCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> categories = Arrays.asList(
                    new Category(null, "Rolls", "Japanese rolls with fresh ingredients", "/images/categories/rolls.jpg", 1, true, null),
                    new Category(null, "Sushi", "Traditional Japanese sushi", "/images/categories/sushi.jpg", 2, true, null),
                    new Category(null, "Sets", "Combo sets for sharing", "/images/categories/sets.jpg", 3, true, null),
                    new Category(null, "Hot Dishes", "Warm Japanese cuisine", "/images/categories/hot.jpg", 4, true, null),
                    new Category(null, "Drinks", "Beverages and drinks", "/images/categories/drinks.jpg", 5, true, null),
                    new Category(null, "Sauces", "Japanese sauces and seasonings", "/images/categories/sauces.jpg", 6, true, null)
            );
            categoryRepository.saveAll(categories);
        }
    }

    private void createDishes() {
        if (dishRepository.count() == 0) {
            List<Category> categories = categoryRepository.findAll();
            Map<String, Category> categoryMap = new HashMap<>();
            for (Category cat : categories) {
                categoryMap.put(cat.getName(), cat);
            }

            List<Dish> dishes = Arrays.asList(
                    // Rolls
                    new Dish(null, "Philadelphia Roll", "Cream cheese, salmon, avocado", new BigDecimal("499"), categoryMap.get("Rolls"), 220, 320, Arrays.asList("rice", "nori", "salmon", "cream cheese", "avocado"), "/images/dishes/philadelphia.jpg", true, null, null, false),
                    new Dish(null, "California Roll", "Crab, avocado, cucumber", new BigDecimal("399"), categoryMap.get("Rolls"), 200, 280, Arrays.asList("rice", "nori", "crab", "avocado", "cucumber"), "/images/dishes/california.jpg", true, null, null, false),
                    new Dish(null, "Dragon Roll", "Eel, cucumber, avocado, eel sauce", new BigDecimal("599"), categoryMap.get("Rolls"), 250, 380, Arrays.asList("rice", "nori", "eel", "cucumber", "avocado", "eel sauce"), "/images/dishes/dragon.jpg", true, null, null, false),
                    new Dish(null, "Spicy Tuna Roll", "Tuna, spicy mayo, cucumber", new BigDecimal("449"), categoryMap.get("Rolls"), 210, 310, Arrays.asList("rice", "nori", "tuna", "spicy mayo", "cucumber"), "/images/dishes/spicy-tuna.jpg", true, null, null, false),
                    new Dish(null, "Rainbow Roll", "Assorted fish over California roll", new BigDecimal("699"), categoryMap.get("Rolls"), 280, 420, Arrays.asList("rice", "nori", "crab", "avocado", "cucumber", "tuna", "salmon", "shrimp"), "/images/dishes/rainbow.jpg", true, null, null, false),

                    // Sushi
                    new Dish(null, "Salmon Nigiri", "Fresh salmon over rice", new BigDecimal("89"), categoryMap.get("Sushi"), 40, 65, Arrays.asList("rice", "salmon"), "/images/dishes/salmon-nigiri.jpg", true, null, null, false),
                    new Dish(null, "Tuna Nigiri", "Fresh tuna over rice", new BigDecimal("99"), categoryMap.get("Sushi"), 40, 70, Arrays.asList("rice", "tuna"), "/images/dishes/tuna-nigiri.jpg", true, null, null, false),
                    new Dish(null, "Eel Nigiri", "Grilled eel with eel sauce", new BigDecimal("119"), categoryMap.get("Sushi"), 45, 85, Arrays.asList("rice", "eel", "eel sauce"), "/images/dishes/eel-nigiri.jpg", true, null, null, false),
                    new Dish(null, "Shrimp Nigiri", "Cooked shrimp over rice", new BigDecimal("79"), categoryMap.get("Sushi"), 35, 55, Arrays.asList("rice", "shrimp"), "/images/dishes/shrimp-nigiri.jpg", true, null, null, false),

                    // Sets
                    new Dish(null, "RedRolls Set", "8 pieces + soup + salad", new BigDecimal("899"), categoryMap.get("Sets"), 450, 680, Arrays.asList("assorted rolls", "miso soup", "salad"), "/images/dishes/redrolls-set.jpg", true, null, null, false),
                    new Dish(null, "Sushi Lovers Set", "12 pieces nigiri + sashimi", new BigDecimal("1299"), categoryMap.get("Sets"), 520, 850, Arrays.asList("assorted nigiri", "sashimi", "wasabi", "ginger"), "/images/dishes/sushi-lovers.jpg", true, null, null, false),
                    new Dish(null, "Family Set", "20 pieces for 2-3 people", new BigDecimal("1599"), categoryMap.get("Sets"), 850, 1350, Arrays.asList("assorted sushi", "rolls", "soup", "salad"), "/images/dishes/family-set.jpg", true, null, null, false),

                    // Hot Dishes
                    new Dish(null, "Chicken Teriyaki", "Grilled chicken with teriyaki sauce", new BigDecimal("599"), categoryMap.get("Hot Dishes"), 320, 450, Arrays.asList("chicken", "teriyaki sauce", "vegetables", "rice"), "/images/dishes/chicken-teriyaki.jpg", true, null, null, false),
                    new Dish(null, "Beef Yakisoba", "Stir-fried noodles with beef", new BigDecimal("549"), categoryMap.get("Hot Dishes"), 350, 520, Arrays.asList("beef", "noodles", "vegetables", "soy sauce"), "/images/dishes/beef-yakisoba.jpg", true, null, null, false),
                    new Dish(null, "Tempura Shrimp", "6 pieces of fried shrimp", new BigDecimal("499"), categoryMap.get("Hot Dishes"), 180, 320, Arrays.asList("shrimp", "tempura batter", "vegetables"), "/images/dishes/tempura-shrimp.jpg", true, null, null, false),

                    // Drinks
                    new Dish(null, "Green Tea", "Traditional Japanese green tea", new BigDecimal("99"), categoryMap.get("Drinks"), 250, 5, Arrays.asList("green tea leaves", "water"), "/images/dishes/green-tea.jpg", true, null, null, false),
                    new Dish(null, "Sake", "Japanese rice wine", new BigDecimal("299"), categoryMap.get("Drinks"), 180, 120, Arrays.asList("rice", "water", "yeast"), "/images/dishes/sake.jpg", true, null, null, false),
                    new Dish(null, "Coca-Cola", "0.33L", new BigDecimal("89"), categoryMap.get("Drinks"), 330, 140, Arrays.asList("water", "sugar", "coca extract"), "/images/dishes/cola.jpg", true, null, null, false),

                    // Sauces
                    new Dish(null, "Soy Sauce", "Traditional soy sauce", new BigDecimal("49"), categoryMap.get("Sauces"), 30, 15, Arrays.asList("soybeans", "salt", "water"), "/images/dishes/soy-sauce.jpg", true, null, null, false),
                    new Dish(null, "Wasabi", "Japanese horseradish", new BigDecimal("39"), categoryMap.get("Sauces"), 10, 8, Arrays.asList("wasabi root", "water"), "/images/dishes/wasabi.jpg", true, null, null, false),
                    new Dish(null, "Pickled Ginger", "Sweet pickled ginger", new BigDecimal("49"), categoryMap.get("Sauces"), 25, 20, Arrays.asList("ginger", "sugar", "vinegar"), "/images/dishes/ginger.jpg", true, null, null, false)
            );

            dishRepository.saveAll(dishes);
        }
    }

    private void createPromotions() {
        if (promotionRepository.count() == 0) {
            List<Promotion> promotions = Arrays.asList(
                new Promotion(null, "Happy Hour -30%", "/images/promotions/happy-hour.jpg", true, 
                    "30% discount on all rolls from 18:00 to 20:00", 1, null),
                new Promotion(null, "Weekend Special", "/images/promotions/weekend.jpg", true, 
                    "Free delivery on orders over 1500 on weekends", 2, null),
                new Promotion(null, "Combo Deal", "/images/promotions/combo.jpg", true, 
                    "Buy 3 sets - get 1 free! Perfect for sharing with friends", 3, null),
                new Promotion(null, "New Customer Bonus", "/images/promotions/new-customer.jpg", true, 
                    "Welcome gift - free wasabi and ginger for first order", 4, null),
                new Promotion(null, "Lunch Time", "/images/promotions/lunch.jpg", true, 
                    "20% off on business lunch sets (12:00-15:00 weekdays)", 5, null)
            );
            
            promotionRepository.saveAll(promotions);
        }
    }

}
