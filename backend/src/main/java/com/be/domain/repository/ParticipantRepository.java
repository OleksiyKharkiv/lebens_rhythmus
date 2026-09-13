package com.be.domain.repository;

import com.be.domain.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    // Security check Tier 1.1 (2026-09-13) — a TEACHER caller must only
    // ever see participants of their OWN groups, same scoping already
    // applied to WorkshopController.byTeacher/GroupController.
    // getGroupsByTeacher/EnrollmentController.participantsForGroup
    // (LR-024). Filtered at the query, not by fetching every participant
    // and filtering in memory — this table holds encrypted child PII
    // (Participant.java), minimizing what leaves the DB matters here.
    List<Participant> findByGroup_Teacher_Id(Long teacherId);
}