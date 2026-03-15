package org.example.deliveryofrolls.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // От кого
            message.setFrom("redrolls@yandex.ru");
            // Кому
            message.setTo(to);
            // Тема письма
            message.setSubject("Восстановление пароля - RedRolls");
            // Текст письма
            message.setText(
                    "Здравствуйте!\n\n" +
                            "Вы запросили восстановление пароля на сайте RedRolls.\n\n" +
                            "Ваш временный пароль: " + tempPassword + "\n\n" +
                            "Рекомендуем сменить его после входа в личном кабинете.\n\n" +
                            "Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.\n\n" +
                            "С уважением,\n" +
                            "Команда RedRolls"
            );
            // Отправляем
            mailSender.send(message);

            log.info("Письмо с паролем отправлено на {}", to);

        } catch (Exception e) {
            log.error("Ошибка отправки письма: {}", e.getMessage());
            throw new RuntimeException("Не удалось отправить письмо", e);
        }
    }
}
