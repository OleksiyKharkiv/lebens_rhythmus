package com.be.domain.repository;

import com.be.domain.entity.Activity;
import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Group;
import com.be.domain.entity.Language;
import com.be.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByActivity(Activity activity);

    List<Group> findByTeacher(Teacher teacher);

    List<Group> findByActiveTrue();

    List<Group> findByActive(boolean active);

    List<Group> findByLanguage(Language language);

    List<Group> findByAgeGroup(AgeGroup ageGroup);

    List<Group> findByActiveAndLanguage(boolean active, Language language);

    List<Group> findByActiveAndAgeGroup(boolean active, AgeGroup ageGroup);
    List<Group> findByWorkshopId(Long workshopId);
}