package com.be;

import com.be.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-025 — OrderController.getById() read a JWT claim ("roles", plural)
 * that JwtUtils never actually mints (the real claim is singular "role").
 * getClaimAsStringList() on a missing claim silently returns null, so the
 * immediately-following .stream() threw a NullPointerException on every
 * single request — the ownership check right after it never ran, for
 * anyone, including real admins. No OrderControllerTest existed before
 * this, which is exactly why it shipped undetected.
 * <p>
 * Real end-to-end test, not a mocked security context (see LR-007's
 * documented gotcha in archive.md — a hand-built jwt() mock can bypass
 * the real claim-reading path entirely and prove nothing).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderOwnershipTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String registerVerifiedUserAndLogin(String email) {
        Map<String, Object> registration = Map.of(
                "email", email,
                "password", "password123",
                "firstName", "Order",
                "lastName", "Test",
                "acceptedTerms", true,
                "privacyPolicyAccepted", true);
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register", registration, Void.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        userRepository.findByEmail(email).ifPresent(u -> {
            u.setEmailVerified(true);
            userRepository.save(u);
        });

        Map<String, Object> login = Map.of("email", email, "password", "password123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login", login, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) loginResponse.getBody().get("token");
    }

    private HttpHeaders bearerJson(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void owner_canReadOwnOrder_strangerCannot_noNpe() {
        long nonce = LocalDateTime.now().getNano();
        String ownerToken = registerVerifiedUserAndLogin("order-owner-" + nonce + "@example.com");
        String strangerToken = registerVerifiedUserAndLogin("order-stranger-" + nonce + "@example.com");

        String orderBody = "{\"orderNumber\":\"ORD-" + nonce + "\"}";
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/orders", HttpMethod.POST,
                new HttpEntity<>(orderBody, bearerJson(ownerToken)), Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number orderId = (Number) createResponse.getBody().get("id");

        // Owner reading their own order must succeed, not 500 (the NPE this ticket fixes).
        ResponseEntity<String> ownerRead = restTemplate.exchange(
                baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(bearerJson(ownerToken)), String.class);
        assertThat(ownerRead.getStatusCode()).isEqualTo(HttpStatus.OK);

        // A different, unrelated user must be denied — not crash, not succeed.
        ResponseEntity<String> strangerRead = restTemplate.exchange(
                baseUrl() + "/api/v1/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(bearerJson(strangerToken)), String.class);
        assertThat(strangerRead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
