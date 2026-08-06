package com.be;

import com.be.domain.entity.enums.Role;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-023 — spring-boot-starter-data-rest was on the classpath with zero
 * exposure restriction: it auto-exported every JpaRepository (User,
 * Participant, Payment, ...) as a full HAL CRUD REST resource at the app
 * root, gated only by SecurityConfig's generic
 * `.anyRequest().authenticated()` — no role check at all. Confirmed live
 * against a real running instance before this fix: a freshly registered
 * plain USER could GET /users (full table dump — bcrypt hashes, decrypted
 * PII) and PATCH /users/{id} {"role":"ADMIN"} to self-escalate to admin,
 * bypassing every @PreAuthorize in every hand-written controller.
 * <p>
 * This is a real end-to-end regression test, not a unit test — it goes
 * through the actual register → login → authenticated-request flow so it
 * proves the fix at the same level the exploit was proven at (a real JWT
 * from a real login, not a mocked security context), and would fail loudly
 * if spring-boot-starter-data-rest (or any equivalent auto-exposure) is
 * ever reintroduced.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringDataRestExposureTest {

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
                "firstName", "Exposure",
                "lastName", "Test",
                "acceptedTerms", true,
                "privacyPolicyAccepted", true);
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register", registration, Void.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Bypass real email delivery — same shortcut used elsewhere in this
        // suite for tests that need a login-ready account, not testing the
        // verification flow itself.
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

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void plainUser_cannotReachSpringDataRestAutoExposedUsersEndpoint() {
        String token = registerVerifiedUserAndLogin("sdr-users-" + LocalDateTime.now().getNano() + "@example.com");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/users", HttpMethod.GET, bearer(token), String.class);

        // 404, not 200 — the mapping must not exist at all, not just be
        // denied. A 401/403 here would still mean the endpoint exists and
        // some caller (e.g. an ADMIN) could reach it — this must be gone
        // entirely, for every role.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminUser_alsoCannotReachSpringDataRestAutoExposedUsersEndpoint() {
        // The fix removes the mapping at the classpath level — role-
        // independent by construction — but proving it literally for ADMIN
        // too, not just structurally, removes all doubt (architect-reviewer
        // nice-to-have, LR-023).
        String email = "sdr-admin-" + LocalDateTime.now().getNano() + "@example.com";
        String token = registerVerifiedUserAndLogin(email);
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setRole(Role.ADMIN);
            userRepository.save(u);
        });
        // Re-login so the JWT's embedded role claim reflects the promotion.
        Map<String, Object> login = Map.of("email", email, "password", "password123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login", login, Map.class);
        String adminToken = (String) loginResponse.getBody().get("token");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/users", HttpMethod.GET, bearer(adminToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void plainUser_cannotReachSpringDataRestAutoExposedParticipantsEndpoint() {
        String token = registerVerifiedUserAndLogin("sdr-participants-" + LocalDateTime.now().getNano() + "@example.com");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/participants", HttpMethod.GET, bearer(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void plainUser_cannotSelfEscalateViaSpringDataRestPatch() {
        String email = "sdr-escalate-" + LocalDateTime.now().getNano() + "@example.com";
        String token = registerVerifiedUserAndLogin(email);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> patchBody = new HttpEntity<>("{\"role\":\"ADMIN\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/users/" + userId, HttpMethod.PATCH, patchBody, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(Role.USER);
    }
}
