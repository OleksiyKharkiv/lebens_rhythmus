package com.be.web.controller;

import com.be.config.CorsProperties;
import com.be.config.SecurityConfig;
import com.be.domain.entity.User;
import com.be.domain.entity.UserNotification;
import com.be.service.UserNotificationService;
import com.be.web.dto.response.UserNotificationResponseDTO;
import com.be.web.mapper.UserNotificationMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LR-089 (security audit, 2026-09-07) — markAsRead(id) used to extract the
 * caller's userId from the JWT and then never check it against anything
 * (the removed code literally said "I'll just call service and assume it's
 * correct for now"). Any authenticated user could mark ANY other user's
 * notification as read by iterating {id}, and the response leaked that
 * person's userId/username/notificationTitle back to the caller. Fixed with
 * the same ownership-check shape as OrderController.getById.
 */
@WebMvcTest(UserNotificationController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class UserNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserNotificationService userNotificationService;

    @MockitoBean
    private UserNotificationMapper userNotificationMapper;

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private MeterRegistry meterRegistry;

    @Test
    void markAsRead_ownNotification_succeeds() throws Exception {
        User owner = User.builder().id(42L).build();
        UserNotification notification = UserNotification.builder().id(7L).user(owner).build();

        when(userNotificationService.getById(7L)).thenReturn(notification);
        when(userNotificationService.markAsRead(7L)).thenReturn(notification);
        when(userNotificationMapper.toResponseDTO(notification))
                .thenReturn(UserNotificationResponseDTO.builder().id(7L).userId(42L).build());

        mockMvc.perform(put("/api/v1/user-notifications/7/read")
                        .with(jwt().jwt(j -> j.claim("id", 42))))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_someoneElsesNotification_isForbidden_andNeverMutated() throws Exception {
        User owner = User.builder().id(99L).build();
        UserNotification notification = UserNotification.builder().id(7L).user(owner).build();

        when(userNotificationService.getById(7L)).thenReturn(notification);

        mockMvc.perform(put("/api/v1/user-notifications/7/read")
                        .with(jwt().jwt(j -> j.claim("id", 42))))
                .andExpect(status().isForbidden());

        // The actual regression this test exists for: the fix must not just
        // return the right status while still mutating the other user's row.
        verify(userNotificationService, never()).markAsRead(eq(7L));
    }

    @Test
    void markAsRead_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(put("/api/v1/user-notifications/7/read"))
                .andExpect(status().isUnauthorized());
    }
}
