package com.be.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender — kept separate from
 * EmailVerificationService so the mail-sending mechanism (Brevo SMTP relay
 * today) can change without touching verification business logic.
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Bitte bestätige deine E-Mail-Adresse — Lebens Rhythmus");
        message.setText("""
                Willkommen bei Lebens Rhythmus!

                Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren:
                %s

                Dieser Link ist 24 Stunden gültig.

                Falls du dich nicht registriert hast, kannst du diese E-Mail ignorieren.
                """.formatted(verificationLink));
        mailSender.send(message);
    }
}
