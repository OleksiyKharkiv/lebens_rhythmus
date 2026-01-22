package com.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Security configuration for the application.
 * <p>
 * - Uses OAuth2 Resource Server (JWT) for authentication.
 * - Converts "role" / "roles" claims from JWT into Spring authorities "ROLE_<X>" so @PreAuthorize("hasRole('ADMIN')") works.
 * - Keeps the JWT as the authentication principal (controllers can read claims and load User from DB when needed).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    /**
     * Password encoder bean used across services for password hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JwtDecoder bean: builds a NimbusJwtDecoder from a symmetric secret (HS256).
     * In production, you may want to use an asymmetric keypair (RS256) — adjust accordingly.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        // secret is expected to be at least 32 bytes for HS256.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Configure a security filter chain:
     * - disable CSRF (API only)
     * - enable CORS (configured separately in WebMvcConfig)
     * - stateless session management
     * - permit open endpoints (login/register, health)
     * - require authentication for everything else
     * - inject a JwtAuthenticationConverter to transform JWT claims into authorities
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Create a converter that will extract GrantedAuthorities from JWT.
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();

        // Use default scope -> authorities converter first (maps "scope"/"scp" -> SCOPE_...).
        JwtGrantedAuthoritiesConverter defaultScopesConverter = new JwtGrantedAuthoritiesConverter();

        // Custom converter: combine scopes and our role/roles claim -> ROLE_<ROLE>
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // 1) Add authorities derived from scopes (if present)
            Collection<org.springframework.security.core.GrantedAuthority> scopeAuth = defaultScopesConverter.convert(jwt);
            Collection<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>(scopeAuth);

            // 2) Add role claim (single string) -> ROLE_<ROLE>
            Object roleClaim = jwt.getClaims().get("role");
            if (roleClaim instanceof String) {
                String r = ((String) roleClaim).trim();
                if (!r.isEmpty()) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r));
                }
            }

            // 3) Add roles claim (list) -> ROLE_<ROLE>
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Iterable<?>) {
                for (Object it : (Iterable<?>) rolesClaim) {
                    if (it != null) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + it));
                    }
                }
            }

            // 4) Some tokens use "authorities" or "Authorities" - support common variations
            Object authoritiesClaim = jwt.getClaims().get("authorities");
            if (authoritiesClaim instanceof Iterable<?>) {
                for (Object it : (Iterable<?>) authoritiesClaim) {
                    if (it != null) {
                        String v = it.toString();
                        // if authority already contains ROLE_ keep, otherwise try to normalize
                        if (v.startsWith("ROLE_") || v.startsWith("SCOPE_")) {
                            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(v));
                        } else {
                            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + v));
                        }
                    }
                }
            }

            return authorities;
        });

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(a -> a
                        // ===== PUBLIC AUTH =====
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register"
                        ).permitAll()

                        // ===== PUBLIC READ =====
                        .requestMatchers(HttpMethod.GET, "/api/v1/workshops/**").permitAll()

                        // ===== ACTUATOR =====
                        .requestMatchers("/actuator/health").permitAll()

                        // ===== STATIC (optional) =====
                        .requestMatchers("/static/**", "/favicon.ico", "/index.html").permitAll()

                        // ===== EVERYTHING ELSE =====
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(o ->
                        o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .build();
    }
}