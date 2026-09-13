package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.domain.repository.UserRepository;
import com.be.web.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * searchUsers() used to be a SQL LIKE derived query
 * (findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase) — broken
 * once firstName/lastName became encrypted (AES-GCM ciphertext never matches
 * a LIKE pattern). Now decrypts transparently via JPA and filters in memory.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private com.be.domain.repository.WorkshopFileRepository workshopFileRepository;
    @Mock
    private com.be.web.mapper.WorkshopFileMapper workshopFileMapper;

    @Test
    void searchUsers_matchesFirstOrLastName_caseInsensitive() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder);

        User alice = User.builder().firstName("Alice").lastName("Schmidt").build();
        User bob = User.builder().firstName("Bob").lastName("Alicante").build();
        User carol = User.builder().firstName("Carol").lastName("Jones").build();
        when(userRepository.findAll()).thenReturn(List.of(alice, bob, carol));

        List<User> result = service.searchUsers("aLiC");

        assertThat(result).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void searchUsers_handlesNullNameFieldsWithoutThrowing() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder);

        User noName = User.builder().build(); // firstName/lastName never set
        when(userRepository.findAll()).thenReturn(List.of(noName));

        assertThat(service.searchUsers("anything")).isEmpty();
    }

    /**
     * LR-089 (security audit, 2026-09-07) — companion to UserMapperTest's
     * toEntity_neverSetsRole: the mapper never sets a role, and THIS is what
     * turns that null into a safe default rather than leaving it null in the
     * database. Together they close the {"role":"ADMIN"} self-registration
     * privilege-escalation hole end to end.
     */
    @Test
    void createUser_nullRole_defaultsToUser() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder);

        User user = User.builder().email("new@example.com").password("plaintext").build();
        when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.createUser(user);

        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void createUser_explicitRole_isNotOverridden() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder);

        User user = User.builder().email("staff@example.com").password("plaintext").role(Role.TEACHER).build();
        when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.createUser(user);

        assertThat(saved.getRole()).isEqualTo(Role.TEACHER);
    }

    // LR-107 — getUserMedia returns media for active enrollments
    @Test
    void getUserMedia_returnsActiveWorkshopMedia() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder, workshopFileRepository, workshopFileMapper);

        com.be.domain.entity.WorkshopFile wf = new com.be.domain.entity.WorkshopFile();
        com.be.web.dto.response.UserMediaDTO dto = com.be.web.dto.response.UserMediaDTO.builder()
                .id(1L)
                .filename("photo.jpg")
                .build();

        when(workshopFileRepository.findMediaByUserIdAndStatusIn(
                org.mockito.ArgumentMatchers.eq(123L),
                org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(wf));
        when(workshopFileMapper.toUserMediaDTO(wf)).thenReturn(dto);

        List<com.be.web.dto.response.UserMediaDTO> result = service.getUserMedia(123L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFilename()).isEqualTo("photo.jpg");
    }
}
