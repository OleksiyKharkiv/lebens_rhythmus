package com.be.config;


import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Small helper utilities to extract common info from Jwt principal.
 * Keep stateless and simple — controllers/services can reuse.
 */
public final class JwtAuthUtils {

    private JwtAuthUtils() { /* util */ }

    /**
     * Extract numeric user id from the JWT "id" claim.
     * Returns null if jwt is null or claims missing/invalid.
     */
    public static Long extractUserId(Jwt jwt) {
        if (jwt == null) return null;
        Object idClaim = jwt.getClaim("id");
        if (idClaim == null) return null;
        if (idClaim instanceof Number) return ((Number) idClaim).longValue();
        try {
            return Long.parseLong(idClaim.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Check role claim. Accepts either a single string claim ("ADMIN")
     * or a collection claim (["USER","ADMIN"]) depending on token shape.
     * Comparison is case-insensitive and tolerates values with/without the "ROLE _" prefix.
     */
    public static boolean hasRole(Jwt jwt, String expectedRole) {
        if (jwt == null || expectedRole == null) return false;
        Object r = jwt.getClaim("role");
        if (r == null) return false;

        // single string
        if (r instanceof String) {
            String s = ((String) r).trim();
            if (s.equalsIgnoreCase(expectedRole)) return true;
            return s.equalsIgnoreCase("ROLE_" + expectedRole);
        }

        // collection
        if (r instanceof Collection) {
            for (Object v : (Collection<?>) r) {
                if (v == null) continue;
                String s = v.toString().trim();
                if (s.equalsIgnoreCase(expectedRole)) return true;
                if (s.equalsIgnoreCase("ROLE_" + expectedRole)) return true;
            }
        }

        return false;
    }
}