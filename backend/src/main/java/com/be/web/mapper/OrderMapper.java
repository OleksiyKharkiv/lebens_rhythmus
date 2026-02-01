package com.be.web.mapper;

import com.be.domain.entity.Order;
import com.be.web.dto.request.OrderRequestDTO;
import com.be.web.dto.response.OrderResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) return null;
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .username(order.getUser() != null ? (order.getUser().getFirstName() + " " + order.getUser().getLastName()) : null)
                .participantId(order.getParticipant() != null ? order.getParticipant().getId() : null)
                .participantName(order.getParticipant() != null ? (order.getParticipant().getFirstName() + " " + order.getParticipant().getLastName()) : null)
                .workshopId(order.getWorkshop() != null ? order.getWorkshop().getId() : null)
                .workshopTitle(order.getWorkshop() != null ? order.getWorkshop().getWorkshopName() : null)
                .eventId(order.getEvent() != null ? order.getEvent().getId() : null)
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : null)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .note(order.getNote())
                .contractId(order.getContract() != null ? order.getContract().getId() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public Order fromRequestDTO(OrderRequestDTO dto) {
        if (dto == null) return null;
        return Order.builder()
                .orderNumber(dto.getOrderNumber())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .quantity(dto.getQuantity())
                .status(dto.getStatus())
                .note(dto.getNote())
                .build();
    }
}
