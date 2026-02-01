package com.be.service;

import com.be.domain.entity.Contract;
import com.be.domain.repository.ContractRepository;
import com.be.web.dto.request.ContractRequestDTO;
import com.be.web.mapper.ContractMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;

    public ContractService(ContractRepository contractRepository, ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.contractMapper = contractMapper;
    }

    @Transactional(readOnly = true)
    public List<Contract> getAll() {
        return contractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Contract getById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
    }

    @Transactional
    public Contract create(ContractRequestDTO dto) {
        Contract contract = contractMapper.fromRequestDTO(dto);
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract update(Long id, ContractRequestDTO dto) {
        Contract existing = getById(id);
        
        if (dto.getContractNumber() != null) existing.setContractNumber(dto.getContractNumber());
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getPartyName() != null) existing.setPartyName(dto.getPartyName());
        if (dto.getContact() != null) existing.setContact(dto.getContact());
        if (dto.getStartDate() != null) existing.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) existing.setEndDate(dto.getEndDate());
        if (dto.getAmount() != null) existing.setAmount(dto.getAmount());
        if (dto.getCurrency() != null) existing.setCurrency(dto.getCurrency());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getContractUrl() != null) existing.setContractUrl(dto.getContractUrl());

        return contractRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!contractRepository.existsById(id)) {
            throw new RuntimeException("Contract not found with id: " + id);
        }
        contractRepository.deleteById(id);
    }
}
