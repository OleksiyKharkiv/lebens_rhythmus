package com.be.domain.repository;

import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Basic CRUD operations
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByEnabledTrue();
    List<User> findByEmailVerifiedTrue();

    // Security related
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedLoginAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockUntil = null WHERE u.email = :email")
    void resetFailedLoginAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.lockUntil = :lockUntil WHERE u.email = :email")
    void lockUserAccount(@Param("email") String email, @Param("lockUntil") LocalDateTime lockUntil);

    // Search operations
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    // Statistics
    long countByRole(Role role);
    long countByEnabledTrue();

    // GDPR compliance
    @Modifying
    @Query("UPDATE User u SET u.enabled = false WHERE u.id = :userId")
    void deactivateUser(@Param("userId") Long userId);
}