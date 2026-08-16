package com.be.web.handler;

import com.be.domain.exception.AlreadyEnrolledException;
import com.be.domain.exception.EmailNotVerifiedException;
import com.be.domain.exception.GroupFullException;
import com.be.domain.exception.InvalidVerificationTokenException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Handler for @Valid validation errors on request body (DTO).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing // keep first if duplicate
                ));

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fieldErrors", fieldErrors);

        log.debug("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * @Valid on a bare List<Dto> request body (e.g. SessionController's
     * replaceSessions, LR-074) throws this instead of
     * MethodArgumentNotValidException above — a different exception type
     * Spring 6.1+ uses for method-level parameter validation that isn't a
     * single @RequestBody object. Without this handler it fell through to
     * the generic Exception catch-all below and returned 500 instead of
     * 400 — found live testing the new endpoint, 2026-08-12.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> messages = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value")
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("messages", messages);

        log.debug("Method validation failed: {}", messages);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Separate handler for ConstraintViolation (e.g., @Validated on request/path parameters).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = v.getPropertyPath().toString();
                            return path.isEmpty() ? "constraint" : path;
                        },
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Constraint violation");
        body.put("violations", violations);

        log.debug("Constraint violations: {}", violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * `@PreAuthorize` denials (authenticated but wrong role) throw
     * AuthorizationDeniedException (Spring Security 6 method-security) or
     * the older AccessDeniedException — neither was handled here, so every
     * such denial across the whole app fell through to handleAll() and
     * came back as 500 instead of 403 (found while testing the new
     * /users/{id}/reactivate endpoint, LR-007 — not specific to it).
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(RuntimeException ex) {
        // LR-031 Phase 1 — no per-path tag deliberately: this app's path
        // set is small and fixed today, but tagging by raw request path is
        // a well-known Micrometer cardinality footgun (a path-traversal
        // probe or similar could mint unbounded tag values) — a plain
        // total is the safe default; add a bounded tag (e.g. controller
        // class) later if per-endpoint breakdown turns out to matter.
        meterRegistry.counter("authz.denied").increment();

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "You do not have permission to perform this action");

        log.debug("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * A request that matches no controller mapping (and no static resource)
     * used to fall through to handleAll() below and come back as a
     * misleading 500 — found while writing LR-023's regression test
     * (verifying spring-boot-starter-data-rest's removal actually made
     * /users//participants stop existing, not just stop being reachable).
     * A truly-unmapped path is a 404, not a server error.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        // LR-031 Phase 1 — retrospectively the most valuable metric in this
        // whole batch: if this had existed 2025-10-21..2026-08-06, LR-023's
        // exploit traffic (GET/PATCH /users, /participants — spring-data-rest
        // paths outside /api/v1/**) would have shown up as an anomaly here
        // from day one instead of sitting undetected for ~9.5 months.
        meterRegistry.counter("http.unmapped_path").increment();

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");

        log.debug("No handler for request: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Fallback — catches everything else and returns 500.
     * <p>
     * LR-029 — used to echo ex.getMessage() verbatim to the client. Full
     * detail is still logged server-side right above (that's correct and
     * stays) — the client-facing body is now a fixed, generic message.
     * The combination of this catch-all + the codebase-wide
     * `orElseThrow(() -> new RuntimeException("X not found with id: " + id))`
     * idiom (dozens of call sites across the service layer) let any
     * authenticated caller distinguish "resource doesn't exist" (message
     * named the entity+id) from "exists but denied" (403, separate
     * handler) from "exists and allowed" (200) — an entity-existence
     * oracle across Order/Enrollment/Contract/Payment/etc. Converting
     * every one of those call sites to a dedicated not-found exception
     * with a uniform 404 is a larger, separate refactor (see follow-up
     * ticket) — this fix closes the actual information leak for all of
     * them at once, regardless of which exception type is thrown.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unhandled exception: ", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal server error");
        body.put("message", "An unexpected error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Responds to bad credentials with unauthorized status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Invalid email or password");

        log.debug("Bad credentials exception: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Correct password, but the account's email was never confirmed. A
     * distinct "code" field (not just the message string) lets the
     * frontend reliably show an actionable "verify your email" prompt
     * instead of a generic invalid-credentials error.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("code", "EMAIL_NOT_VERIFIED");
        body.put("message", ex.getMessage());

        log.debug("Login blocked, email not verified: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * /verify-email's token didn't match any stored hash, or matched but
     * has expired.
     */
    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("code", "INVALID_VERIFICATION_TOKEN");
        body.put("message", ex.getMessage());

        log.debug("Invalid verification token: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * LR-084 — EnrollmentService's atomic capacity check found nothing left.
     * 409 Conflict: the request is well-formed, but the resource's current
     * state (no open spots) rejects it. Distinct code so the frontend can
     * show a clean, expected "this is full" message instead of a
     * server-error-looking one — see GroupFullException's own javadoc for
     * why this specifically was found in review (Course has no pre-emptive
     * capacity check on the frontend, unlike Workshop, so this handler is
     * that page's everyday full-course path, not just a race-condition edge
     * case).
     */
    @ExceptionHandler(GroupFullException.class)
    public ResponseEntity<Map<String, Object>> handleGroupFull(GroupFullException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("code", "GROUP_FULL");
        body.put("message", ex.getMessage());

        log.debug("Enrollment rejected, group full: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * LR-084 — user already has an Enrollment for the requested Workshop/
     * Course (uk_user_workshop/uk_user_course). Same reasoning as
     * GroupFullException above.
     */
    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyEnrolled(AlreadyEnrolledException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("code", "ALREADY_ENROLLED");
        body.put("message", ex.getMessage());

        log.debug("Enrollment rejected, already enrolled: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}