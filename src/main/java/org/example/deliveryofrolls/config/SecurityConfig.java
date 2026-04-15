package org.example.deliveryofrolls.config;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Отключаем CSRF для API
                )
                .authorizeHttpRequests(auth -> auth
                        // Разрешаем доступ к загруженным файлам для всех
                        .requestMatchers("/uploads/**").permitAll()
                        // ===== API ЭНДПОИНТЫ ДЛЯ КАРТЫ И ЗОН ДОСТАВКИ =====
                        .requestMatchers("/api/delivery-zones/active").permitAll()  // Активные зоны для карты
                        .requestMatchers("/api/delivery-zones/check").permitAll()
                        .requestMatchers("/api/pickup-points/active").permitAll()   // Точки самовывоза
                        .requestMatchers("/api/delivery-zones/admin/**").hasRole("ADMIN") // Админские эндпоинты
                        // Публичные страницы
                        .requestMatchers("/", "/cart/**", "/delivery", "/contacts",
                                "/register", "/cart/**", "/home", "/error", "/order/**",
                                "/promotions", "/password/**").permitAll()
                        .requestMatchers("/api/promo/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Админка только для ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Профиль для авторизованных
                        .requestMatchers("/profile/**").authenticated()

                        // Все остальное - только авторизованные
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/home")
                        .invalidateHttpSession(true)           // Убить сессию на сервере
                        .clearAuthentication(true)             // Очистить аутентификацию
                        .deleteCookies("JSESSIONID", "remember-me") // Удалить ВСЕ cookies аутентификации
                        .permitAll()

                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(2592000) // 30 дней
                        .userDetailsService(userDetailsService)
                        .alwaysRemember(false)   // Не запоминать принудительно
                )
                .sessionManagement(session -> session
                        // Редирект при истекшей сессии
                        .invalidSessionUrl("/login?timeout")
                        // Явное разрешение мультисессий
                        .maximumSessions(-1)

                );

        return http.build();
    }
}
