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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
//                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы
                        .requestMatchers("/", "/menu/**", "/cart/**", "/delivery", "/contacts",
                                "/register").permitAll()
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
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)           // Убить сессию на сервере
                        .clearAuthentication(true)             // Очистить аутентификацию
                        .deleteCookies("JSESSIONID", "remember-me") // Удалить ВСЕ cookies аутентификации
                        .permitAll()

                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(2592000) // 30 дней
                        .userDetailsService(userDetailsService)
                )
                .sessionManagement(session -> session
                        // Редирект при истекшей сессии (вместо 401 ошибки)
                        .invalidSessionUrl("/login?timeout")

                        // Явное разрешение мультисессий
                        .maximumSessions(-1)

                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")  // Показывать эту страницу при отказе в доступе
                );

        return http.build();
    }
}
