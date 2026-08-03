package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.exception.InvalidVerificationTokenException;
import com.be.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MailService mailService;

    private EmailVerificationService service() {
        return new EmailVerificationService(userRepository, mailService, "https://tlab29.com", 24);
    }

    @Test
    void sendVerificationEmail_setsHashAndExpiry_andEmailsALinkContainingThePlaintextToken() {
        User user = User.builder().id(1L).email("a@example.com").build();

        service().sendVerificationEmail(user);

        assertThat(user.getVerificationTokenHash()).isNotBlank();
        assertThat(user.getVerificationTokenExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        verify(userRepository).save(user);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerificationEmail(eq("a@example.com"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("https://tlab29.com/verify-email?token=");
        // The hash stored on the user must never equal the plaintext token
        // embedded in the link — that would defeat the whole point of hashing.
        String plaintextToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        assertThat(user.getVerificationTokenHash()).isNotEqualTo(plaintextToken);
    }

    @Test
    void sendVerificationEmail_mailSendingFails_doesNotPropagate() {
        // A mail outage (or SMTP not configured yet) must not blow up
        // registration — the account and its token are already saved by
        // the time mail sending is attempted.
        User user = User.builder().id(1L).email("a@example.com").build();
        doThrow(new MailSendException("smtp down")).when(mailService).sendVerificationEmail(anyString(), anyString());

        service().sendVerificationEmail(user); // must not throw

        verify(userRepository).save(user);
    }

    @Test
    void verifyToken_roundTrip_marksEmailVerifiedAndClearsToken() {
        User user = User.builder().id(1L).email("a@example.com").emailVerified(false).build();
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        EmailVerificationService service = service();
        service.sendVerificationEmail(user);
        verify(mailService).sendVerificationEmail(anyString(), linkCaptor.capture());
        String token = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        service.verifyToken(token);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationTokenHash()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();
    }

    @Test
    void verifyToken_unknownToken_throwsInvalidVerificationToken() {
        when(userRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().verifyToken("some-random-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void verifyToken_expiredToken_throwsInvalidVerificationToken() {
        // Reuse the real generation path (not a hand-picked hash string) so
        // this actually exercises the expiry check, not a hash mismatch —
        // hash() is private, so a fake "somehash" would never match what
        // verifyToken() itself computes from a plaintext token.
        User user = User.builder().id(1L).email("a@example.com").emailVerified(false).build();
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        EmailVerificationService service = service();
        service.sendVerificationEmail(user);
        verify(mailService).sendVerificationEmail(anyString(), linkCaptor.capture());
        String token = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        user.setVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1)); // simulate expiry
        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyToken(token))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }
}
