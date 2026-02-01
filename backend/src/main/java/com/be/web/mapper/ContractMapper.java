package com.be.web.mapper;

import com.be.domain.entity.Contract;
import com.be.web.dto.request.ContractRequestDTO;
import com.be.web.dto.response.ContractResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponseDTO toResponseDTO(Contract contract) {
        if (contract == null) return null;
        return ContractResponseDTO.builder()
                .id(contract.getId())
                .contractNumber(contract.getContractNumber())
                .title(contract.getTitle())
                .partyName(contract.getPartyName())
                .contact(contract.getContact())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .amount(contract.getAmount())
                .currency(contract.getCurrency())
                .status(contract.getStatus())
                .contractUrl(contract.getContractUrl())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }

    public Contract fromRequestDTO(ContractRequestDTO dto) {
        if (dto == null) return null;
        return Contract.builder()
                .contractNumber(dto.getContractNumber())
                .title(dto.getTitle())
                .partyName(dto.getPartyName())
                .contact(dto.getContact())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .contractUrl(dto.getContractUrl())
                .build();
    }
}
