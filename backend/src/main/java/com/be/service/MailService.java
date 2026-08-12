package com.be.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender — kept separate from
 * EmailVerificationService so the mail-sending mechanism (Brevo SMTP relay
 * today) can change without touching verification business logic.
 * <p>
 * HTML + plain-text multipart (not SimpleMailMessage/plain-text-only) —
 * found live 2026-08-12 (beta feedback): a bare URL with no styling reads
 * as spam/careless, the first real impression of the site. Every major
 * client (Gmail, Outlook, Apple Mail) strips unsupported CSS silently, so
 * the button below sticks to the historically email-safe subset: inline
 * styles only, no flexbox/grid, a plain `display:inline-block` anchor
 * rather than a background-image or web font.
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
        String subject = "Bitte bestätige deine E-Mail-Adresse — Lebens Rhythmus";
        String plainText = """
                Willkommen bei Lebens Rhythmus!

                Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren:
                %s

                Dieser Link ist 24 Stunden gültig. Nach der Bestätigung kannst du dich direkt auf tlab29.com einloggen.

                Falls du dich nicht registriert hast, kannst du diese E-Mail ignorieren.
                """.formatted(verificationLink);

        String html = """
                <!DOCTYPE html>
                <html lang="de">
                <body style="margin:0;padding:0;background-color:#2b1b29;font-family:'Nunito Sans',Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#2b1b29;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" style="max-width:480px;background-color:#3d2a38;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="padding:32px 32px 8px 32px;">
                              <p style="margin:0;font-size:20px;font-weight:700;color:#f7eedd;">Lebens Rhythmus</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:8px 32px 24px 32px;">
                              <p style="margin:0 0 16px 0;font-size:16px;color:#f7eedd;">Willkommen! Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren.</p>
                              <table role="presentation" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="border-radius:999px;background-color:#f0a93b;">
                                    <a href="%s" target="_blank" style="display:inline-block;padding:14px 32px;font-size:16px;font-weight:700;color:#2b1b29;text-decoration:none;border-radius:999px;">E-Mail bestätigen</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 0 0;font-size:13px;color:#c9b8a8;">Dieser Link ist 24 Stunden gültig. Nach der Bestätigung kannst du dich direkt auf tlab29.com einloggen.</p>
                              <p style="margin:16px 0 0 0;font-size:12px;color:#c9b8a8;">Funktioniert der Button nicht? Link kopieren: <a href="%s" style="color:#2fbf9e;">%s</a></p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 32px 24px 32px;border-top:1px solid #4a3644;">
                              <p style="margin:0;font-size:12px;color:#c9b8a8;">Falls du dich nicht registriert hast, kannst du diese E-Mail ignorieren.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(verificationLink, verificationLink, verificationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            mailSender.send(message);
        } catch (MessagingException e) {
            // Same non-fatal-to-the-caller reasoning as before: wrap in an
            // unchecked exception so EmailVerificationService's existing
            // MailException catch (its own comment explains why that must
            // never 500 the registration/resend request) still applies —
            // MimeMessageHelper's checked MessagingException doesn't
            // subclass MailException on its own.
            throw new MailParseException(e);
        }
    }
}
