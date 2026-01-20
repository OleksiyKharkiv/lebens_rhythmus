package com.be.domain.repository;

import com.be.domain.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    List<Workshop> findByStartDateAfterOrderByStartDateAsc(LocalDate after);

    List<Workshop> findByTeacherId(Long teacherId);

    List<Workshop> findByWorkshopNameContainingIgnoreCase(String q);
}