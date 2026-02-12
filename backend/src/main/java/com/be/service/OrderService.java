package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.repository.*;
import com.be.web.dto.request.OrderRequestDTO;
import com.be.web.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final WorkshopRepository workshopRepository;
    private final EventRepository eventRepository;
    private final ContractRepository contractRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ParticipantRepository participantRepository,
                        WorkshopRepository workshopRepository,
                        EventRepository eventRepository,
                        ContractRepository contractRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.workshopRepository = workshopRepository;
        this.eventRepository = eventRepository;
        this.contractRepository = contractRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Transactional
    public Order create(OrderRequestDTO dto, Long userId) {
        Order order = orderMapper.fromRequestDTO(dto);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        order.setUser(user);

        if (dto.getParticipantId() != null) {
            Participant participant = participantRepository.findById(dto.getParticipantId())
                    .orElseThrow(() -> new RuntimeException("Participant not found"));
            order.setParticipant(participant);
        }
        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            order.setWorkshop(workshop);
        }
        if (dto.getEventId() != null) {
            Event event = eventRepository.findById(dto.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            order.setEvent(event);
        }
        if (dto.getContractId() != null) {
            Contract contract = contractRepository.findById(dto.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            order.setContract(contract);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order update(Long id, OrderRequestDTO dto) {
        Order existing = getById(id);
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getNote() != null) existing.setNote(dto.getNote());
        // ... other updates if needed
        return orderRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(id);
    }
}