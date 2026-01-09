package com.be.domain.repository;

import com.be.domain.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndWorkshopId(Long userId, Long workshopId);

    List<Enrollment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Enrollment> findByWorkshopId(Long workshopId);

    List<Enrollment> findByGroupId(Long groupId);
}