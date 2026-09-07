package com.be.web.mapper;

import com.be.domain.entity.User;
import com.be.web.dto.request.UserRegistrationDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-089 (security audit, 2026-09-07) — toEntity() used to read a
 * client-supplied `role` field straight off UserRegistrationDTO and set it
 * on the new User, on the public, unauthenticated /auth/register endpoint.
 * Anyone could register with {"role":"ADMIN"} and get an admin account with
 * zero authentication. The DTO field is gone now; this test guards against
 * a future refactor reintroducing it (e.g. a well-meaning "let's map every
 * field automatically" change).
 */
class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toEntity_neverSetsRole_regardlessOfDtoContents() {
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .email("attacker@example.com")
                .password("password123")
                .firstName("Attacker")
                .lastName("Test")
                .build();

        User result = mapper.toEntity(dto);

        // UserService.createUser() is what actually defaults a null role to
        // Role.USER (see UserServiceTest) - the mapper's job is simply to
        // never set one, leaving that decision entirely server-side.
        assertThat(result.getRole()).isNull();
        assertThat(result.getEmail()).isEqualTo("attacker@example.com");
    }
}
