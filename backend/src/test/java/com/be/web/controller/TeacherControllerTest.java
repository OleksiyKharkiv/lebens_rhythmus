package com.be.web.controller;

import com.be.config.CorsProperties;
import com.be.config.SecurityConfig;
import com.be.domain.entity.Teacher;
import com.be.service.TeacherService;
import com.be.web.dto.TeacherInfoDTO;
import com.be.web.mapper.TeacherMapper;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization and scoping tests for TeacherController (LR-099).
 */
@WebMvcTest(TeacherController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private TeacherMapper teacherMapper;

    @MockitoBean(answers = Answers.RETURNS_MOCKS)
    private MeterRegistry meterRegistry;

    @Test
    void getAll_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_asAdmin_succeeds() throws Exception {
        when(teacherService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/teachers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getById_asUser_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_asTeacherWithProfile_returnsOk() throws Exception {
        Teacher teacher = Teacher.builder().id(10L).firstName("Olena").lastName("Test").build();
        TeacherInfoDTO dto = TeacherInfoDTO.builder().id(10L).firstName("Olena").lastName("Test").build();

        when(teacherService.resolveTeacherIdForUser(5L)).thenReturn(Optional.of(10L));
        when(teacherService.getById(10L)).thenReturn(teacher);
        when(teacherMapper.toInfoDTO(teacher)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/teachers/me")
                        .with(jwt()
                                .jwt(j -> j.claim("id", 5L))
                                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.firstName").value("Olena"));
    }

    @Test
    void getMe_asTeacherWithoutProfile_returnsNotFound() throws Exception {
        when(teacherService.resolveTeacherIdForUser(5L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/teachers/me")
                        .with(jwt()
                                .jwt(j -> j.claim("id", 5L))
                                .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/me"))
                .andExpect(status().isUnauthorized());
    }
}
