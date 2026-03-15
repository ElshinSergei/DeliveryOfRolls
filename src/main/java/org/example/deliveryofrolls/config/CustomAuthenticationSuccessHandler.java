package org.example.deliveryofrolls.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.service.CartService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final CartService cartService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Объединяем корзины при входе
        cartService.mergeCarts(session, userDetails);

        log.info("✅ Пользователь {} успешно вошел в систему", userDetails.getUsername());

        String contextPath = request.getContextPath();
        response.sendRedirect(contextPath + "/home");
    }
}
