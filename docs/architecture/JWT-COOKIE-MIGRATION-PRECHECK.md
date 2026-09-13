# Architecture Pre-Check: JWT Migration from LocalStorage to HttpOnly SameSite Cookies (LR-108)

**Date:** 2026-09-13  
**Status:** Phase 1 Active (Dual-Stack Authentication)  
**Related Tickets:** LR-101 (CORS & HTTPS Redirect), LR-108 (Dual Auth & Pre-Check), LR-109 (HSTS Header)  
**Applicable Standards:** OWASP Session Management Cheat Sheet, DSGVO Art. 32 (Security of Processing), LR-ADR-007 (Stateless JWT Architecture).

---

## 1. Executive Summary & Threat Model

### Problem Statement
Until LR-108, the client application (`frontend-svelte`) stored the active Bearer JWT in `localStorage.getItem('authToken')` with a 24-hour lifetime (`app.jwt.expiration`). 

Storage in `localStorage` makes credentials accessible to any JavaScript running in the same origin:
- **XSS Vulnerability**: Any script injection (via third-party script, DOM XSS, or template injection) can directly exfiltrate `localStorage.getItem('authToken')`, allowing an attacker to impersonate the user until token expiration.
- **Ambient Theft**: Unlike cookies with `HttpOnly`, `localStorage` has no mechanism to hide tokens from the DOM API.

### Solution: Migration to HttpOnly SameSite Cookies
Migrate session token transmission to `Set-Cookie` with the following attributes:
- `HttpOnly`: Inaccessible to `document.cookie` or any client-side JavaScript execution, providing hard mitigation against token exfiltration via XSS.
- `Secure`: Transmitted only over encrypted TLS (HTTPS) connections.
- `SameSite=Lax`: Automatically blocks the browser from attaching cookies on cross-site requests initiating state-changing HTTP methods (`POST`, `PUT`, `DELETE`, `PATCH`).
- `Path=/`: Restricts scope to application paths.
- `Max-Age`: Mirrored from token expiration (`86400` seconds / 24h).

---

## 2. Cross-Site Request Forgery (CSRF) Analysis

Transitioning from explicit header authentication (`Authorization: Bearer <token>`) to ambient cookie authentication introduces theoretical CSRF exposure. The attack surface has been comprehensively verified:

1. **Idempotent GET Endpoints (Verified Clean)**:
   - All `@GetMapping` endpoints across all 20 controllers in `backend/src/main/java/com/be/web/controller/` are strictly read-only and idempotent. There are zero state-mutating GET operations.
2. **`SameSite=Lax` Protection**:
   - Modern browsers (Chrome, Firefox, Safari, Edge) treat `SameSite=Lax` as the default. Under `SameSite=Lax`, cookies are only sent on top-level navigation (`GET` initiated by a user clicking a link). They are strictly withheld on cross-origin `POST`, `PUT`, `DELETE`, or `PATCH` requests (such as malicious form submissions or AJAX calls from external sites).
3. **Network Isolation & Origin Verification**:
   - The backend service is not directly reachable from the public internet (Kubernetes `ClusterIP` + `NetworkPolicy`).
   - All public ingress traffic is reverse-proxied through Traefik.
   - `X-Forwarded-Proto` is trusted because Traefik is the sole external gateway.
   - Strict CORS configuration (`WebMvcConfig.java` / `CorsProperties.java`) allows credentials only from explicit verified origins (`https://www.tlab29.com`, `https://tlab29.com`, `http://localhost:5173`).

---

## 3. Transport Security State

- **HTTPS Redirection**: Active. Ingress route is bound to `lr-dev-redirect-to-https@kubernetescrd` middleware (`devops/helm/lr-app/templates/middleware-redirect.yaml`), forcing all HTTP traffic to HTTPS (`301 Moved Permanently`).
- **HSTS Gap (Identified by Review)**: The `Strict-Transport-Security` header is currently not emitted by Ingress or Nginx. This leaves a small theoretical window on the very first plaintext request before redirection occurs.
  - **Resolution**: Ticket **LR-109** has been registered in `docs/tickets/tickets.md` to add a Traefik headers middleware configuring `stsSeconds: 31536000`, `stsIncludeSubdomains: true`, and `stsPreload: true`.

---

## 4. Token Invalidation & Logout Semantics (Crucial Security Note)

> [!WARNING]
> **Logout is Cookie Clearing, NOT Cryptographic Revocation**:
> Calling `POST /api/v1/auth/logout` emits a `Set-Cookie` with `Max-Age=0`, which instructs the client browser to delete its local `authToken` cookie.
> 
> Because the application adheres to stateless JWT architecture (LR-ADR-007) and does not currently maintain a distributed revocation blacklist (e.g. Redis blocklist or DB-backed session table), any previously issued token remains cryptographically valid until its expiration timestamp (`exp`). If a token was copied or retained prior to logout, it will continue to authenticate via the `Authorization: Bearer <token>` header until natural expiry. True server-side revocation would require a Redis token blacklist or reduced token lifetime (e.g. 15-minute access token + refresh token rotation), which is out of scope for Phase 1.

---

## 5. Phased Zero-Downtime Migration Plan

### Phase 1: Dual-Mode Authentication (Implemented in LR-108)
- **Backend Resolution**: `CookieBearerTokenResolver` inspects `Authorization: Bearer <token>` first. If absent, it checks `Cookie: authToken=<token>`.
- **Backend Emission**: `POST /api/v1/auth/login` returns the full `UserLoginResponseDTO` with the token string, and simultaneously attaches `Set-Cookie: authToken=...; HttpOnly; SameSite=Lax; Path=/`.
- **Backend Logout**: `POST /api/v1/auth/logout` emits `Set-Cookie: authToken=; Max-Age=0`.
- **Frontend Fetch**: `request()` sets `credentials: 'include'`.
- **Zero Downtime**: Existing sessions or clients sending `Authorization: Bearer` continue working without disruption.

### Phase 2: Client Migration away from LocalStorage (Next Sprint)
- `frontend-svelte` stops reading `localStorage.getItem('authToken')` for request headers.
- Requests rely purely on browser cookie attachment via `credentials: 'include'`.
- `localStorage` is restricted to non-sensitive UI metadata (`userData` with role and display name for immediate layout hydration, verified asynchronously against `/users/me`).

### Phase 3: Cookie-Only Hardening (Final Phase)
- Deprecate returning the raw `token` string in `UserLoginResponseDTO`.
- Deprecate header-based resolution for browser-facing endpoints.
