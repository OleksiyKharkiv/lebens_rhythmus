package com.be.domain.repository;

import com.be.domain.entity.WorkshopFile;
import com.be.domain.entity.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkshopFileRepository extends JpaRepository<WorkshopFile, Long> {

    // LR-107 — fetches media files for workshops the user is actively enrolled in
    // (CONFIRMED or PENDING). Uses JOIN FETCH to avoid N+1 queries. Explicit
    // e.workshop IS NOT NULL ensures stable compilation in Hibernate 6.6.x.
    @Query("SELECT DISTINCT f FROM WorkshopFile f " +
            "JOIN FETCH f.workshop w " +
            "JOIN w.enrollments e " +
            "WHERE e.user.id = :userId " +
            "AND e.workshop IS NOT NULL " +
            "AND e.status IN :activeStatuses " +
            "ORDER BY f.id DESC")
    List<WorkshopFile> findMediaByUserIdAndStatusIn(
            @Param("userId") Long userId,
            @Param("activeStatuses") Collection<EnrollmentStatus> activeStatuses);
}