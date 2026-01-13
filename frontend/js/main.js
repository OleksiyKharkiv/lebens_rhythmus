// ========== API BASE ==========
window.API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api/v1'
    : 'https://api.tlab29.com/api/v1';

// ========== AUTH HELPERS ==========
function isAuthenticated() {
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');
    return !!(token && expiry && Date.now() < Number(expiry));
}

function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('authToken');
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
}

function logout() {
    localStorage.clear();
    location.href = '/pages/login/login.html';
}

window.isAuthenticated = isAuthenticated;
window.getAuthHeaders = getAuthHeaders;
window.logout = logout;

// ========== UI HELPERS ==========
function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    if (!menu) return;
    menu.style.display = (menu.style.display === 'flex') ? 'none' : 'flex';
}

function scrollToSection(id) {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth' });
}

// close mobile menu by default
document.addEventListener('DOMContentLoaded', () => {
    const menu = document.getElementById('mobileMenu');
    if (menu) menu.style.display = 'none';
});