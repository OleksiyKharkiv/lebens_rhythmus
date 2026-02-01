package com.be.service;

import com.be.domain.entity.Teacher;
import com.be.domain.repository.TeacherRepository;
import com.be.web.dto.request.TeacherRequestDTO;
import com.be.web.mapper.TeacherMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherMapper teacherMapper;

    public TeacherService(TeacherRepository teacherRepository, TeacherMapper teacherMapper) {
        this.teacherRepository = teacherRepository;
        this.teacherMapper = teacherMapper;
    }

    @Transactional(readOnly = true)
    public List<Teacher> getAll() {
        return teacherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Teacher getById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + id));
    }

    @Transactional
    public Teacher create(TeacherRequestDTO dto) {
        Teacher teacher = teacherMapper.fromRequestDTO(dto);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher update(Long id, TeacherRequestDTO dto) {
        Teacher existing = getById(id);
        
        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        existing.setApproved(dto.isApproved());
        if (dto.getBioDe() != null) existing.setBioDe(dto.getBioDe());
        if (dto.getBioEn() != null) existing.setBioEn(dto.getBioEn());
        if (dto.getBioUa() != null) existing.setBioUa(dto.getBioUa());
        existing.setActive(dto.isActive());

        return teacherRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new RuntimeException("Teacher not found");
        }
        teacherRepository.deleteById(id);
    }
}
