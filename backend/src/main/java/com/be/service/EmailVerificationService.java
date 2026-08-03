package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.exception.InvalidVerificationTokenException;
import com.be.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and verifies the one-time email-confirmation link sent at
 * registration (LR: User.emailVerified existed since V1 but nothing ever
 * set it true or sent any email at all — this closes that gap).
 * <p>
 * Only the SHA-256 hash of the token is ever persisted (User.
 * verificationTokenHash) — the plaintext token exists only in the email
 * itself and transiently here while generating/verifying, same reasoning
 * as password hashing: a DB/backup leak must not hand out working
 * verification links.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository userRepository;
    private final MailService mailService;
    private final String frontendUrl;
    private final long tokenTtlHours;

    public EmailVerificationService(
            UserRepository userRepository,
            MailService mailService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.email-verification.token-ttl-hours}") long tokenTtlHours) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
        this.tokenTtlHours = tokenTtlHours;
    }

    /**
     * Generates a fresh token, stores its hash on the user, and emails the
     * plaintext token as a verification link. Overwrites any previous
     * unclaimed token — only the latest one sent is ever valid.
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        String plaintextToken = generateToken();
        user.setVerificationTokenHash(hash(plaintextToken));
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(tokenTtlHours));
        userRepository.save(user);

        String link = frontendUrl + "/verify-email?token=" + plaintextToken;
        try {
            mailService.sendVerificationEmail(user.getEmail(), link);
        } catch (MailException ex) {
            // Deliberately not rethrown: a mail outage (or SMTP not
            // configured yet — SMTP_USERNAME/SMTP_PASSWORD default to
            // empty, see application.properties) must not 500 the whole
            // registration/resend request. The account and its token are
            // already saved above, so a later "resend verification" retry
            // (once mail delivery actually works) still succeeds with a
            // fresh token. Logged at ERROR — loud on purpose, not silently
            // swallowed like numi's mocked-SMTP incident that went
            // unnoticed for a long time specifically because nothing
            // logged the failure.
            log.error("Failed to send verification email to {}: {}", user.getEmail(), ex.getMessage(), ex);
        }
    }

    /**
     * Looks up the user by the token's hash, checks it hasn't expired, marks
     * the account verified, and clears the token so it can't be replayed.
     */
    @Transactional
    public void verifyToken(String plaintextToken) {
        User user = userRepository.findByVerificationTokenHash(hash(plaintextToken))
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid or already-used verification link"));

        if (user.getVerificationTokenExpiresAt() == null
                || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationTokenException("Verification link has expired");
        }

        user.setEmailVerified(true);
        user.setVerificationTokenHash(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String plaintextToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintextToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory JDK algorithm — this can't actually happen.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
