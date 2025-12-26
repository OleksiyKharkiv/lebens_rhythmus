/**
 * Login/Registration Page Script for Lebens Rhythmus
 * JWT-based authentication with full UX feedback
 */

document.addEventListener('DOMContentLoaded', function () {

    // ========== CONFIG ==========
    const API_BASE_URL = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api'
        : 'https://api.tlab29.com/api';

    const ENDPOINTS = {
        LOGIN: `${API_BASE_URL}/auth/login`,
        REGISTER: `${API_BASE_URL}/auth/register`
    };

    // ========== DOM ==========
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    const loginEmail = document.getElementById('loginEmail');
    const loginPassword = document.getElementById('loginPassword');

    const registerFirstName = document.getElementById('registerFirstName');
    const registerLastName = document.getElementById('registerLastName');
    const registerEmail = document.getElementById('registerEmail');
    const registerPassword = document.getElementById('registerPassword');
    const registerConfirmPassword = document.getElementById('registerConfirmPassword');
    const acceptTerms = document.getElementById('acceptTerms');
    const acceptPrivacy = document.getElementById('acceptPrivacy');

    const notificationsContainer = createNotificationContainer();

    // ========== AUTH CHECK ==========
    checkAuthStatus();

    // ========== PASSWORD TOGGLE ==========
    document.querySelectorAll('.toggle-password').forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = btn.dataset.target;
            const input = document.getElementById(targetId);
            if (input.type === 'password') input.type = 'text';
            else input.type = 'password';
        });
    });

    // ========== LOGIN ==========
    if (loginForm) {
        loginForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const email = loginEmail.value.trim();
            const password = loginPassword.value;

            if (!validateEmail(email)) {
                showNotification('Bitte geben Sie eine gültige E-Mail-Adresse ein.', 'error');
                return;
            }
            if (password.length < 6) {
                showNotification('Passwort muss mindestens 6 Zeichen lang sein.', 'error');
                return;
            }

            const btn = loginForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            btn.textContent = 'Wird eingeloggt...';

            try {
                const response = await fetch(ENDPOINTS.LOGIN, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({email, password})
                });

                const data = await response.json();

                if (!response.ok) {
                    showNotification(data.message || 'Login fehlgeschlagen.', 'error');
                    return;
                }

                persistAuth(data);
                showNotification('Erfolgreich eingeloggt!', 'success');
                redirectBasedOnRole(data.role);

            } catch (err) {
                console.error(err);
                showNotification('Netzwerkfehler.', 'error');
            } finally {
                btn.disabled = false;
                btn.textContent = 'Login';
            }
        });
    }

    // ========== REGISTER ==========
    if (registerForm) {
        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            if (!acceptTerms.checked || !acceptPrivacy.checked) {
                showNotification('Bitte akzeptieren Sie alle Bedingungen.', 'error');
                return;
            }

            if (registerPassword.value !== registerConfirmPassword.value) {
                showNotification('Passwörter stimmen nicht überein.', 'error');
                return;
            }

            const btn = registerForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            btn.textContent = 'Wird registriert...';

            try {
                const payload = {
                    firstName: registerFirstName.value.trim(),
                    lastName: registerLastName.value.trim(),
                    email: registerEmail.value.trim(),
                    password: registerPassword.value
                };

                const response = await fetch(ENDPOINTS.REGISTER, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(payload)
                });

                const data = await response.json();

                if (!response.ok) {
                    showNotification(data.message || 'Registrierung fehlgeschlagen.', 'error');
                    return;
                }

                persistAuth(data);
                showNotification('Registrierung erfolgreich!', 'success');
                redirectBasedOnRole(data.role);

            } catch (err) {
                console.error(err);
                showNotification('Netzwerkfehler.', 'error');
            } finally {
                btn.disabled = false;
                btn.textContent = 'Registrieren';
            }
        });
    }

    // ========== HELPERS ==========
    function checkAuthStatus() {
        if (!isAuthenticated()) {
            clearAuthData();
            return;
        }

        const raw = localStorage.getItem('userData');
        if (!raw) {
            clearAuthData();
            return;
        }

        try {
            const user = JSON.parse(raw);
            redirectBasedOnRole(user.role || 'USER');
        } catch {
            clearAuthData();
        }
    }

    function persistAuth(data) {
        localStorage.setItem('authToken', data.token);
        localStorage.setItem('userData', JSON.stringify({
            id: data.id,
            email: data.email,
            firstName: data.firstName,
            lastName: data.lastName,
            role: data.role
        }));
        localStorage.setItem('tokenExpiry', (Date.now() + data.expiresIn * 1000).toString());
    }

    function clearAuthData() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userData');
        localStorage.removeItem('tokenExpiry');
    }

    function validateEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function redirectBasedOnRole(role) {
        const target =
            role === 'ADMIN' ? '../admin/dashboard.html'
                : role === 'TEACHER' ? '../teacher/dashboard.html'
                    : '../dashboard/dashboard.html';

        setTimeout(() => location.replace(target), 500);
    }

    // ========== NOTIFICATIONS ==========
    function createNotificationContainer() {
        const container = document.createElement('div');
        container.id = 'notificationContainer';
        container.style.position = 'fixed';
        container.style.top = '20px';
        container.style.right = '20px';
        container.style.zIndex = '9999';
        container.style.width = '300px';
        document.body.appendChild(container);
        return container;
    }

    function showNotification(msg, type) {
        const notif = document.createElement('div');
        notif.textContent = msg;
        notif.style.padding = '10px 15px';
        notif.style.marginBottom = '10px';
        notif.style.borderRadius = '5px';
        notif.style.color = '#fff';
        notif.style.boxShadow = '0 2px 6px rgba(0,0,0,0.2)';
        notif.style.opacity = '0.95';
        notif.style.transition = 'opacity 0.5s ease';
        if (type === 'success') notif.style.backgroundColor = '#4CAF50';
        else if (type === 'error') notif.style.backgroundColor = '#f44336';
        else notif.style.backgroundColor = '#2196F3';

        notificationsContainer.appendChild(notif);
        setTimeout(() => {
            notif.style.opacity = '0';
            setTimeout(() => notificationsContainer.removeChild(notif), 500);
        }, 3000);
    }
});

// ========== GLOBAL AUTH ==========
function isAuthenticated() {
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');
    return token && expiry && Date.now() < Number(expiry);
}

function getAuthHeaders() {
    return {
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
        'Content-Type': 'application/json'
    };
}

function logout() {
    localStorage.clear();
    location.href = '/pages/login/login.html';
}

window.isAuthenticated = isAuthenticated;
window.getAuthHeaders = getAuthHeaders;
window.logout = logout;