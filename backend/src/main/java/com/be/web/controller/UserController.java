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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "https://tlab29.com")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // ========== USER PROFILE ENDPOINTS ==========

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUserProfile(@AuthenticationPrincipal User currentUser) {
        UserProfileDTO profile = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateCurrentUserProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserUpdateDTO updateDTO) {

        User updatedUser = userService.updateUser(currentUser.getId(), updateDTO);
        UserProfileDTO profileDTO = userMapper.toProfileDTO(updatedUser);
        return ResponseEntity.ok(profileDTO);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserPasswordUpdateDTO passwordDTO) {

        boolean success = userService.changePassword(
                currentUser.getId(),
                passwordDTO.getCurrentPassword(),
                passwordDTO.getNewPassword()
        );

        if (!success) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        return ResponseEntity.ok("Password updated successfully");
    }

    @PostMapping("/me/verify-email")
    public ResponseEntity<?> verifyEmail(@AuthenticationPrincipal User currentUser) {
        userService.verifyEmail(currentUser.getId());
        return ResponseEntity.ok("Email verification initiated");
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deactivateAccount(@AuthenticationPrincipal User currentUser) {
        userService.deleteUser(currentUser.getId());
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
    ) {}
}