package com.be.web.controller;

import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.service.UserService;
import com.be.web.dto.request.UserPasswordUpdateDTO;
import com.be.web.dto.request.UserUpdateDTO;
import com.be.web.dto.response.UserBasicDTO;
import com.be.web.dto.response.UserProfileDTO;
import com.be.web.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // ========== USER PROFILE ENDPOINTS ==========

    // ========== USER PROFILE ENDPOINTS (updated to use Jwt principal) ==========

    /**
     * GET /api/v1/users/me
     * Use Jwt principal (resource-server) and read user id from claim "id".
     * If jwt is null or missing claim -> return 401 (unauthorized).
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/v1/users/me
     * Update current user's profile — same JWT -> extract id.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateDTO updateDTO) {

        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User updatedUser = userService.updateUser(userId, updateDTO);
        UserProfileDTO profileDTO = userMapper.toProfileDTO(updatedUser);
        return ResponseEntity.ok(profileDTO);
    }

    /**
     * PUT /api/v1/users/me/password
     * Change password for current user.
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserPasswordUpdateDTO passwordDTO) {

        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        boolean success = userService.changePassword(
                userId,
                passwordDTO.getCurrentPassword(),
                passwordDTO.getNewPassword()
        );

        if (!success) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        return ResponseEntity.ok("Password updated successfully");
    }

    /**
     * POST /api/v1/users/me/verify-email
     */
    @PostMapping("/me/verify-email")
    public ResponseEntity<?> verifyEmail(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userService.verifyEmail(userId);
        return ResponseEntity.ok("Email verification initiated");
    }

    /**
     * DELETE /api/v1/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<?> deactivateAccount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserIdFromJwt(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userService.deleteUser(userId);
        return ResponseEntity.ok("Account deactivated successfully");
    }


    // ========== ADMIN ENDPOINTS ==========

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserBasicDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserBasicDTO> userDTOs = userMapper.toBasicDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserBasicDTO>> searchUsers(@RequestParam String query) {
        List<User> users = userService.searchUsers(query);
        List<UserBasicDTO> userDTOs = userMapper.toBasicDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserBasicDTO>> getUsersByRole(@PathVariable Role role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserBasicDTO> userDTOs = userMapper.toBasicDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long userId) {
        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserBasicDTO> updateUserRole(
            @PathVariable Long userId,
            @RequestParam Role role) {

        User updatedUser = userService.updateUserRole(userId, role);
        UserBasicDTO userDTO = userMapper.toBasicDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deactivated successfully");
    }

    // ========== STATISTICS ENDPOINTS ==========

    /**
     * Returns user statistics: total, active, and by role
     */
    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStatistics() {
        long totalUsers = userService.getTotalUserCount();
        long activeUsers = userService.getActiveUserCount();
        long userCount = userService.getUserCountByRole(Role.USER);
        long teacherCount = userService.getUserCountByRole(Role.TEACHER);
        long adminCount = userService.getUserCountByRole(Role.ADMIN);

        return ResponseEntity.ok(new UserStatistics(totalUsers, activeUsers, userCount, teacherCount, adminCount));
    }

    // Helper class for statistics
    private record UserStatistics(
            long totalUsers,
            long activeUsers,
            long userCount,
            long teacherCount,
            long adminCount
    ) {
    }

    /**
     * Helper method to extract user ID from JWT token.
     * Returns null if JWT is missing or ID claim is not present.
     */
    private Long extractUserIdFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object idClaim = jwt.getClaim("id");
        if (idClaim == null) {
            return null;
        }
        return (idClaim instanceof Number) ? ((Number) idClaim).longValue() : Long.parseLong(idClaim.toString());
    }
}