package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Order;
import com.be.service.OrderService;
import com.be.web.dto.request.OrderRequestDTO;
import com.be.web.dto.response.OrderResponseDTO;
import com.be.web.mapper.OrderMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<OrderResponseDTO>> getAll() {
        List<Order> orders = orderService.getAll();
        return ResponseEntity.ok(orders.stream()
                .map(orderMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER') or isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Order order = orderService.getById(id);
        // Basic security check: user can only see their own order unless they are admin/owner
        Long userId = JwtAuthUtils.extractUserId(jwt);
        boolean isAdmin = jwt.getClaimAsStringList("roles").stream().anyMatch(r -> r.equals("ADMIN") || r.equals("BUSINESS_OWNER"));
        
        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(orderMapper.toResponseDTO(order));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponseDTO> create(@Valid @RequestBody OrderRequestDTO dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtAuthUtils.extractUserId(jwt);
        Order created = orderService.create(dto, userId);
        return ResponseEntity.status(201).body(orderMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<OrderResponseDTO> update(@PathVariable Long id, @Valid @RequestBody OrderRequestDTO dto) {
        Order updated = orderService.update(id, dto);
        return ResponseEntity.ok(orderMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
