package com.be.service;

import com.be.domain.entity.Order;
import com.be.domain.entity.User;
import com.be.domain.repository.*;
import com.be.web.dto.request.OrderRequestDTO;
import com.be.web.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CODING_PROTOCOL.md §4b — found 2026-08-16 (architect-reviewer, during the
 * LR-084 registration-architecture roundtable): OrderRequestDTO.status was
 * copied straight from the client with no override, letting any
 * authenticated user POST an order already marked "PAID". This test
 * exercises the fix: create() must always force PENDING regardless of what
 * the client sent.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ContractRepository contractRepository;

    private OrderService service() {
        return new OrderService(orderRepository, userRepository, participantRepository,
                workshopRepository, courseRepository, eventRepository, contractRepository, new OrderMapper());
    }

    @Test
    void create_clientSuppliedStatusPAID_isIgnored_orderStartsAtPending() {
        User user = User.builder().id(1L).email("test@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // A malicious/naive client trying to self-mark its own order paid.
        OrderRequestDTO dto = OrderRequestDTO.builder()
                .amount(new BigDecimal("50.00"))
                .currency("EUR")
                .quantity(1)
                .status("PAID")
                .build();

        Order created = service().create(dto, 1L);

        assertThat(created.getStatus()).isEqualTo("PENDING");
    }
}
