package com.be.domain.repository;

import com.be.domain.entity.WorkshopFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkshopFileRepository extends JpaRepository<WorkshopFile, Long> {
}
