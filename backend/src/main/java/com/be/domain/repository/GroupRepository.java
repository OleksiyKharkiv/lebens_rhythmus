package com.be.domain.repository;

import com.be.domain.entity.Activity;
import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Group;
import com.be.domain.entity.Language;
import com.be.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // LR-084 — atomic, WHERE-guarded decrement/increment on the
    // previously-unused capacityLeft column, closing the capacity
    // check-then-save race in EnrollmentService.enroll() (a separate
    // getEnrolledCount()>=getCapacity() check + save() is not atomic across
    // two concurrent transactions). Return value is rows affected: 0 means
    // "no capacity left" for the decrement (caller must reject the
    // enrollment), same pattern as UserRepository's login-lockout queries.
    @Modifying
    @Query("UPDATE Group g SET g.capacityLeft = g.capacityLeft - 1 WHERE g.id = :id AND g.capacityLeft > 0")
    int decrementCapacityLeft(@Param("id") Long id);

    // Symmetric release on cancel/expire — only ever called for an
    // enrollment that previously succeeded in decrementCapacityLeft, so no
    // upper-bound guard is needed (paired calls, not independently
    // user-triggerable).
    @Modifying
    @Query("UPDATE Group g SET g.capacityLeft = g.capacityLeft + 1 WHERE g.id = :id")
    void incrementCapacityLeft(@Param("id") Long id);

    List<Group> findByActivity(Activity activity);

    List<Group> findByTeacher(Teacher teacher);

    List<Group> findByActiveTrue();

    List<Group> findByActive(boolean active);

    List<Group> findByLanguage(Language language);

    List<Group> findByAgeGroup(AgeGroup ageGroup);

    List<Group> findByActiveAndLanguage(boolean active, Language language);

    List<Group> findByActiveAndAgeGroup(boolean active, AgeGroup ageGroup);

    List<Group> findByWorkshopId(Long workshopId);

    List<Group> findByCourseId(Long courseId);
}