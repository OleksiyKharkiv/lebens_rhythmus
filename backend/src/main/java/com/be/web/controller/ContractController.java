package com.be.web.controller;

import com.be.domain.entity.Contract;
import com.be.service.ContractService;
import com.be.web.dto.request.ContractRequestDTO;
import com.be.web.dto.response.ContractResponseDTO;
import com.be.web.mapper.ContractMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractMapper contractMapper;

    public ContractController(ContractService contractService, ContractMapper contractMapper) {
        this.contractService = contractService;
        this.contractMapper = contractMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<ContractResponseDTO>> getAll() {
        List<Contract> contracts = contractService.getAll();
        return ResponseEntity.ok(contracts.stream()
                .map(contractMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ContractResponseDTO> getById(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        return ResponseEntity.ok(contractMapper.toResponseDTO(contract));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ContractResponseDTO> create(@Valid @RequestBody ContractRequestDTO dto) {
        Contract created = contractService.create(dto);
        return ResponseEntity.status(201).body(contractMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ContractResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ContractRequestDTO dto) {
        Contract updated = contractService.update(id, dto);
        return ResponseEntity.ok(contractMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
