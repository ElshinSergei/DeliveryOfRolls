package org.example.deliveryofrolls.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // связывание URL-путей с физическими файлами на диске
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.home") + "/sushi-uploads/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));;
    }
}
