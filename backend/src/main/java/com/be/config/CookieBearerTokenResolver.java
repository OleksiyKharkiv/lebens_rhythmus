package com.be.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * LR-108: Dual-mode BearerTokenResolver supporting both traditional Authorization header
 * and modern HttpOnly SameSite cookies.
 * <p>
 * Precedence order:
 * 1. "Authorization: Bearer <token>" header via DefaultBearerTokenResolver (explicit caller credential)
 * 2. Fallback to HttpOnly session cookie (default name: "authToken")
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    public static final String DEFAULT_COOKIE_NAME = "authToken";

    private final BearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
    private final String cookieName;

    public CookieBearerTokenResolver() {
        this(DEFAULT_COOKIE_NAME);
    }

    public CookieBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        // 1. Check Authorization header first
        String token = defaultResolver.resolve(request);
        if (token != null && !token.isBlank()) {
            return token;
        }

        // 2. Check Cookie fallback
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }

        return null;
    }

    public String getCookieName() {
        return cookieName;
    }
}
