package com.be.domain.repository;

import com.be.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // LR-024 — User and Teacher are separate entities with no FK between
    // them, linked only by matching email (see Teacher.email's own
    // uniqueness comment). This is the one place that link is resolved
    // server-side, for ownership checks — was previously only done
    // client-side (frontend-svelte teacher/+page.svelte), which is a UX
    // convenience, not a security boundary.
    Optional<Teacher> findByEmail(String email);
}