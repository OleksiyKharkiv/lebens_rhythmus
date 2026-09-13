package com.be.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for LR-108 CookieBearerTokenResolver.
 * Pure JUnit 5 test using MockHttpServletRequest without Spring context overhead.
 */
class CookieBearerTokenResolverTest {

    private CookieBearerTokenResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CookieBearerTokenResolver();
    }

    @Test
    @DisplayName("Resolves token from Authorization Bearer header")
    void resolvesTokenFromAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-jwt-token");

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("header-jwt-token");
    }

    @Test
    @DisplayName("Resolves token from cookie when Authorization header is absent")
    void resolvesTokenFromCookieWhenHeaderAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("authToken", "cookie-jwt-token"));

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("cookie-jwt-token");
    }

    @Test
    @DisplayName("Authorization header takes precedence over cookie when both are present")
    void headerTakesPrecedenceOverCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-jwt-token");
        request.setCookies(new Cookie("authToken", "cookie-jwt-token"));

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo("header-jwt-token");
    }

    @Test
    @DisplayName("Returns null when neither header nor cookies are present")
    void returnsNullWhenNeitherHeaderNorCookiePresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("Returns null when cookie value is blank or empty")
    void returnsNullWhenCookieValueIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("authToken", "   "));

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("Ignores cookies with different names")
    void ignoresUnrelatedCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("session_id", "12345"), new Cookie("theme", "dark"));

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }

    @Test
    @DisplayName("Supports custom cookie name")
    void supportsCustomCookieName() {
        CookieBearerTokenResolver customResolver = new CookieBearerTokenResolver("customAuth");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("customAuth", "custom-jwt-token"));

        String token = customResolver.resolve(request);

        assertThat(token).isEqualTo("custom-jwt-token");
    }
}
