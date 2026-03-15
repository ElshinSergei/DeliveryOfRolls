package org.example.deliveryofrolls.config;

import com.github.javafaker.Faker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class Config {

    @Bean
    public Faker faker() {
        return new Faker(new Locale("ru"));
    }
}
