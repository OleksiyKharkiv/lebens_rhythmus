package com.be.web.controller;

import com.be.config.CorsProperties;
import com.be.config.SecurityConfig;
import com.be.service.VenueService;
import com.be.web.mapper.VenueMapper;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization tests for VenueController (LR-099).
 */
@WebMvcTest(VenueController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private VenueMapper venueMapper;

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private MeterRegistry meterRegistry;

    @Test
    void getAll_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/venues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_asAdmin_succeeds() throws Exception {
        when(venueService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/venues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getById_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/venues/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/venues"))
                .andExpect(status().isUnauthorized());
    }
}
