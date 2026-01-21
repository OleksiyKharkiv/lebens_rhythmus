// main.js
// Global base + auth helpers shared across frontend pages.
// Exports: window.API_BASE_URL, window.isAuthenticated, window.getAuthHeaders, window.logout, window.fetchJson

// tolerant dev host check (localhost or 127.0.0.1)
window.API_BASE_URL = ['localhost', '127.0.0.1'].includes(window.location.hostname)
    ? 'http://localhost:8080/api/v1'
    : 'https://api.tlab29.com/api/v1';

// ========== AUTH HELPERS ==========

/**
 * Return true if there's a token and it hasn't expired.
 * tokenExpiry is stored as a number (ms since epoch) in localStorage.
 */
function isAuthenticated() {
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');
    return !!(token && expiry && Date.now() < Number(expiry));
}

/**
 * Build headers for authenticated requests.
 * Always returns an object (useful to spread into fetch options).
 */
function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    try {
        const token = localStorage.getItem('authToken');
        if (token) headers['Authorization'] = `Bearer ${token}`;
    } catch (e) {
        // localStorage might throw in some environments — fail gracefully
        console.warn('getAuthHeaders: localStorage unavailable', e);
    }
    return headers;
}

/**
 * Clear session storage and redirect to the login page.
 * Use when a user logs out or when a token is invalid/expired.
 */
function logout() {
    localStorage.clear();
    // keep relative path consistent with pages structure
    location.href = '/pages/login/login.html';
}

// ========== fetch wrapper ==========

/**
 * fetchJson wraps fetch:
 * - merges provided headers with auth headers
 * - on 401 it clears localStorage and redirects to log in
 * - parses JSON responses and throws descriptive Error on non-ok
 *
 * Usage:
 *   const data = await fetchJson(`${API_BASE_URL}/users/me`, { method: 'GET' });
 */
async function fetchJson(url, opts = {}) {
    const finalOpts = Object.assign({}, opts);
    finalOpts.headers = Object.assign({}, finalOpts.headers || {}, getAuthHeaders());

    const res = await fetch(url, finalOpts);

    // Automatic handling for unauthorized responses => force re-login
    if (res.status === 401) {
        try { localStorage.clear(); } catch (e) { /* ignore */ }
        // redirect to the login page (do not attempt further processing)
        window.location.href = '/pages/login/login.html';
        throw new Error('Unauthorized'); // caller can catch if needed
    }

    // No content
    if (res.status === 204) return null;

    const text = await res.text();

    // If response body empty but status ok -> return null
    if (!text) {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return null;
    }

    // Try parse JSON; provide useful error messages
    try {
        const json = JSON.parse(text);
        if (!res.ok) {
            const msg = json.message || json.error || `HTTP ${res.status}`;
            const err = new Error(msg);
            err.status = res.status;
            throw err;
        }
        return json;
    } catch (e) {
        // Not JSON
        if (!res.ok) throw new Error(`HTTP ${res.status}: ${text}`);
        return text;
    }
}

// export to global
window.isAuthenticated = isAuthenticated;
window.getAuthHeaders = getAuthHeaders;
window.logout = logout;
window.fetchJson = fetchJson;

// ========== small UI helpers kept in main.js ==========
function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    if (!menu) return;
    menu.style.display = (menu.style.display === 'flex') ? 'none' : 'flex';
}

function scrollToSection(id) {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth' });
}

// ensure mobile menu hidden on load
document.addEventListener('DOMContentLoaded', () => {
    const menu = document.getElementById('mobileMenu');
    if (menu) menu.style.display = 'none';
});