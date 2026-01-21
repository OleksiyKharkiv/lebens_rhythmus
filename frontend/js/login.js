/**
 * Login / Registration – Lebens Rhythmus
 * Safe JWT auth handling
 */
document.addEventListener('DOMContentLoaded', function () {

    // ========== ENDPOINTS ==========
    const ENDPOINTS = {
        LOGIN: `${window.API_BASE_URL}/auth/login`,
        REGISTER: `${window.API_BASE_URL}/auth/register`
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

    // ========== LOGIN ==========
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const email = loginEmail.value.trim();
            const password = loginPassword.value;

            if (!validateEmail(email)) {
                showNotification('Ungültige E-Mail-Adresse.', 'error');
                return;
            }

            const btn = loginForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            btn.textContent = 'Login...';

            try {
                const response = await fetch(ENDPOINTS.LOGIN, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({email, password})
                });

                if (!response.ok) {
                    handleErrorResponse(response);
                    return;
                }

                const data = await response.json();
                persistAuth(data);

                showNotification('Login erfolgreich!', 'success');
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
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            if (!acceptTerms.checked || !acceptPrivacy.checked) {
                showNotification('Bitte Bedingungen akzeptieren.', 'error');
                return;
            }

            if (registerPassword.value !== registerConfirmPassword.value) {
                showNotification('Passwörter stimmen nicht überein.', 'error');
                return;
            }

            const btn = registerForm.querySelector('button[type="submit"]');
            btn.disabled = true;
            btn.textContent = 'Registrieren...';

            try {
                const response = await fetch(ENDPOINTS.REGISTER, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        firstName: registerFirstName.value.trim(),
                        lastName: registerLastName.value.trim(),
                        email: registerEmail.value.trim(),
                        password: registerPassword.value
                    })
                });

                if (!response.ok) {
                    handleErrorResponse(response);
                    return;
                }

                const data = await response.json();
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
        // ========== SHOW / HIDE PASSWORD ==========
        const togglePasswordButtons = document.querySelectorAll('.toggle-password');
        togglePasswordButtons.forEach(btn => {
            const targetId = btn.getAttribute('data-target');
            const input = document.getElementById(targetId);

            btn.addEventListener('click', () => {
                if (input.type === 'password') {
                    input.type = 'text';
                    btn.textContent = '🙈'; // меняем иконку
                } else {
                    input.type = 'password';
                    btn.textContent = '👁️';
                }
            });
        });

    }

    // ========== HELPERS ==========
    function handleErrorResponse(response) {
        if (response.status === 401) {
            showNotification('E-Mail oder Passwort falsch.', 'error');
            return;
        }

        response.text().then(text => {
            if (!text) {
                showNotification(`Fehler ${response.status}`, 'error');
                return;
            }
            try {
                const json = JSON.parse(text);
                showNotification(json.message || 'Fehler.', 'error');
            } catch {
                showNotification(text, 'error');
            }
        });
    }

    function persistAuth(data) {
        localStorage.setItem('authToken', data.token);
        localStorage.setItem('tokenExpiry', Date.now() + data.expiresIn * 1000);
        localStorage.setItem('userData', JSON.stringify({
            id: data.id,
            email: data.email,
            role: data.role
        }));
    }

    function checkAuthStatus() {
        if (!window.isAuthenticated()) return;
        try {
            const user = JSON.parse(localStorage.getItem('userData'));
            redirectBasedOnRole(user.role);
        } catch {
            localStorage.clear();
        }
    }

    function redirectBasedOnRole(role) {
        const target =
            role === 'ADMIN' ? '../admin/dashboard.html'
                : role === 'TEACHER' ? '../teacher/dashboard.html'
                    : '../dashboard/dashboard.html';
        location.replace(target);
    }

    function validateEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    // ========== NOTIFICATIONS ==========
    function createNotificationContainer() {
        const el = document.createElement('div');
        el.style.position = 'fixed';
        el.style.top = '20px';
        el.style.right = '20px';
        el.style.zIndex = '9999';
        document.body.appendChild(el);
        return el;
    }

    function showNotification(msg, type) {
        const n = document.createElement('div');
        n.textContent = msg;
        n.style.marginBottom = '10px';
        n.style.padding = '10px';
        n.style.borderRadius = '5px';
        n.style.color = '#fff';
        n.style.background =
            type === 'error' ? '#f44336'
                : type === 'success' ? '#4CAF50'
                    : '#2196F3';
        notificationsContainer.appendChild(n);
        setTimeout(() => n.remove(), 3000);
    }
});