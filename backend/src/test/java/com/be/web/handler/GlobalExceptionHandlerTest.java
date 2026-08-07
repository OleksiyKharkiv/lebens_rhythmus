package com.be.web.handler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-029 — the catch-all used to echo ex.getMessage() verbatim to the
 * client, an information-disclosure/entity-existence-oracle issue
 * (combined with the codebase-wide "X not found with id: Y" idiom).
 */
class GlobalExceptionHandlerTest {

    // Real in-memory registry, same reasoning as AuthServiceTest — cheap,
    // and lets these tests assert the LR-031 counters actually increment.
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(meterRegistry);

    @Test
    void handleAll_doesNotEchoExceptionMessageToClient() {
        RuntimeException sensitive = new RuntimeException("Order not found with id: 42");

        ResponseEntity<Map<String, Object>> response = handler.handleAll(sensitive);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message"))
                .isEqualTo("An unexpected error occurred")
                .asString()
                .doesNotContain("42");
    }

    @Test
    void handleAccessDenied_incrementsAuthzDeniedCounter() {
        handler.handleAccessDenied(new AccessDeniedException("nope"));

        assertThat(meterRegistry.counter("authz.denied").count()).isEqualTo(1.0);
    }

    @Test
    void handleNoResourceFound_incrementsUnmappedPathCounter() {
        // LR-031 Phase 1's most retrospectively valuable metric — this is
        // exactly the signal that would have surfaced LR-023's exploit
        // traffic on day one.
        handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.GET, "/users//participants"));

        assertThat(meterRegistry.counter("http.unmapped_path").count()).isEqualTo(1.0);
    }
}
