package com.be.web.controller;

import com.be.config.CorsProperties;
import com.be.config.SecurityConfig;
import com.be.service.UserService;
import com.be.web.mapper.UserMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for PUT /users/{id}/reactivate (admin panel, LR-007) —
 * added because no counterpart to DELETE (soft-deactivate) existed at all.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    // LR-031 Phase 1 — GlobalExceptionHandler now requires a MeterRegistry
    // (security-metric counters), which @WebMvcTest's thin slice context
    // doesn't autoconfigure. RETURNS_MOCKS (not the default RETURNS_DEFAULTS)
    // because .counter(...) must return a non-null Counter for the chained
    // .increment() call the handler makes — this slice doesn't assert on
    // metrics, it just needs the bean graph to construct without NPEs.
    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private MeterRegistry meterRegistry;

    @Test
    void reactivateUser_asAdmin_succeeds() throws Exception {
        // SecurityMockMvcRequestPostProcessors.jwt() derives authorities
        // from its own default converter (reads "scope"/"scp" only), NOT
        // from SecurityConfig's custom JwtAuthenticationConverter — a
        // .claim("role", "ADMIN") here is silently ignored for authorization
        // purposes. Authorities must be set explicitly to actually exercise
        // hasRole(...) (found while writing this test: both this case and
        // the non-admin case below returned 500 until fixed, which is what
        // surfaced the separate GlobalExceptionHandler bug, see LR-007).
        mockMvc.perform(put("/api/v1/users/7/reactivate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(userService).reactivateUser(7L);
    }

    @Test
    void reactivateUser_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/7/reactivate")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactivateUser_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/users/7/reactivate"))
                .andExpect(status().isUnauthorized());
    }
}
