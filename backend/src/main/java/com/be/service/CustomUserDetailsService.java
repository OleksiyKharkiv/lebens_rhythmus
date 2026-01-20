package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        // Adapts domain user to Spring Security user
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .authorities("ROLE_" + u.getRole())
                .build();
    }
}