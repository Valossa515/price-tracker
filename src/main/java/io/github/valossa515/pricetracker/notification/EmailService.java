package io.github.valossa515.pricetracker.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
public class EmailService {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendPriceAlert(
            String to,
            String productName,
            BigDecimal targetPrice,
            BigDecimal observedPrice,
            String productUrl) {

        NumberFormat brl = NumberFormat.getCurrencyInstance(BR);
        String name = (productName != null && !productName.isBlank()) ? productName : "(sem nome)";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Preço atingido: " + name);
        msg.setText("""
                Olá!

                O preço do produto que você está monitorando atingiu o valor desejado.

                Produto: %s
                Preço atual: %s
                Seu alvo: %s

                Acesse agora: %s

                — price-tracker
                """.formatted(name, brl.format(observedPrice), brl.format(targetPrice), productUrl));

        try {
            mailSender.send(msg);
            log.info("Price alert email sent to {} (product={})", to, name);
        } catch (Exception e) {
            log.error("Failed to send price alert email to {}: {}", to, e.getMessage());
        }
    }
}
