/**
 * Login/Registration Page Script for Lebens Rhythmus
 * Integrates with Spring Boot Backend using JWT authentication
 * Based on provided ERM, User entity, Auth flow, and deployment config
 */

document.addEventListener('DOMContentLoaded', function() {
    // ========== CONFIGURATION ==========
    const API_BASE_URL = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api'
        : 'https://api.tlab29.com/api';

    const ENDPOINTS = {
        LOGIN: `${API_BASE_URL}/auth/login`,
        REGISTER: `${API_BASE_URL}/auth/register`,
        VERIFY_TOKEN: `${API_BASE_URL}/auth/verify-token`  // Assuming this endpoint exists
    };

    // ========== DOM ELEMENTS ==========
    const loginForm = document.querySelector('.login-form');
    const registerForm = document.querySelector('.register-form');
    const loginEmail = loginForm?.querySelector('input[type="email"]');
    const loginPassword = loginForm?.querySelector('input[type="password"]');

    const registerName = registerForm?.querySelector('input[type="text"]');
    const registerEmail = registerForm?.querySelectorAll('input[type="email"]')[0];
    const registerPassword = registerForm?.querySelectorAll('input[type="password"]')[0];
    const registerConfirmPassword = registerForm?.querySelectorAll('input[type="password"]')[1];

    // Check GDPR checkbox (add this to your register form in HTML)
    let gdprCheckbox = null;
    const gdprDiv = document.createElement('div');
    gdprDiv.className = 'gdpr-checkbox';
    gdprDiv.innerHTML = `
        <label style="display: flex; align-items: center; margin: 10px 0; font-size: 0.9em;">
            <input type="checkbox" id="gdpr-accept" required>
            <span style="margin-left: 8px;">
                Ich akzeptiere die 
                <a href="../impressum/impressum.html" target="_blank" style="color: #0066cc;">
                    AGB und Datenschutzbestimmungen
                </a>
            </span>
        </label>
    `;

    // Insert GDPR checkbox before register button
    if (registerForm) {
        const registerBtn = registerForm.querySelector('button[type="submit"]');
        registerForm.insertBefore(gdprDiv, registerBtn);
        gdprCheckbox = document.getElementById('gdpr-accept');
    }

    // ========== AUTH STATUS CHECK ==========
    checkAuthStatus();

    // ========== LOGIN FORM HANDLER ==========
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const email = loginEmail.value.trim();
            const password = loginPassword.value;

            // Frontend validation
            if (!validateEmail(email)) {
                showNotification('Bitte geben Sie eine gültige E-Mail-Adresse ein.', 'error');
                return;
            }

            if (password.length < 6) {
                showNotification('Passwort muss mindestens 6 Zeichen lang sein.', 'error');
                return;
            }

            try {
                // Disable button during request
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.textContent;
                submitBtn.textContent = 'Wird eingeloggt...';
                submitBtn.disabled = true;

                // Prepare request according to UserLoginRequestDTO structure
                const loginData = {
                    email: email,
                    password: password
                };

                const response = await fetch(ENDPOINTS.LOGIN, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(loginData)
                });

                const data = await response.json();

                if (response.ok) {
                    // Save token and user data based on UserLoginResponseDTO
                    localStorage.setItem('authToken', data.token);
                    localStorage.setItem('userData', JSON.stringify({
                        id: data.id,
                        email: data.email,
                        firstName: data.firstName,
                        lastName: data.lastName,
                        role: data.role,
                        participants: data.participants || [],
                        teachers: data.teachers || []
                    }));

                    // Save token expiry (convert expiresIn to timestamp)
                    const expiryTime = Date.now() + (data.expiresIn || 86400000);
                    localStorage.setItem('tokenExpiry', expiryTime.toString());

                    showNotification('Erfolgreich eingeloggt! Willkommen zurück!', 'success');

                    // Redirect to dashboard based on user role
                    setTimeout(() => {
                        redirectBasedOnRole(data.role);
                    }, 1500);

                } else {
                    // Handle specific error cases
                    if (response.status === 401) {
                        showNotification('Ungültige Anmeldedaten. Bitte überprüfen Sie Email und Passwort.', 'error');
                    } else if (response.status === 423) {
                        showNotification('Konto gesperrt. Zu viele fehlgeschlagene Versuche. Bitte versuchen Sie es später.', 'error');
                    } else {
                        showNotification(data.message || 'Login fehlgeschlagen. Bitte versuchen Sie es erneut.', 'error');
                    }
                }

            } catch (error) {
                console.error('Login error:', error);
                showNotification('Netzwerkfehler. Bitte überprüfen Sie Ihre Internetverbindung.', 'error');
            } finally {
                // Re-enable button
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }
        });
    }

    // ========== REGISTRATION FORM HANDLER ==========
    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const name = registerName.value.trim();
            const email = registerEmail.value.trim();
            const password = registerPassword.value;
            const confirmPassword = registerConfirmPassword.value;

            // Extract first and last name
            const nameParts = name.split(' ');
            const firstName = nameParts[0] || '';
            const lastName = nameParts.slice(1).join(' ') || '';

            // Validation
            if (!name || nameParts.length < 2) {
                showNotification('Bitte geben Sie Vor- und Nachname ein.', 'error');
                return;
            }

            if (!validateEmail(email)) {
                showNotification('Bitte geben Sie eine gültige E-Mail-Adresse ein.', 'error');
                return;
            }

            if (password.length < 6) {
                showNotification('Passwort muss mindestens 6 Zeichen lang sein.', 'error');
                return;
            }

            if (password !== confirmPassword) {
                showNotification('Passwörter stimmen nicht überein.', 'error');
                return;
            }

            if (!gdprCheckbox?.checked) {
                showNotification('Bitte akzeptieren Sie die AGB und Datenschutzbestimmungen.', 'error');
                return;
            }

            try {
                // Disable button during request
                const submitBtn = registerForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.textContent;
                submitBtn.textContent = 'Wird registriert...';
                submitBtn.disabled = true;

                // Prepare request according to UserRegistrationDTO structure
                const registrationData = {
                    email: email,
                    password: password,
                    firstName: firstName,
                    lastName: lastName,
                    phone: '', // Optional field
                    birthDate: null, // Optional field
                    role: 'USER', // Default role
                    address: '', // Optional
                    city: '', // Optional
                    zipCode: '', // Optional
                    country: 'Deutschland', // Default
                    acceptedTerms: true,
                    privacyPolicyAccepted: true
                };

                const response = await fetch(ENDPOINTS.REGISTER, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(registrationData)
                });

                const data = await response.json();

                if (response.ok) {
                    // Auto-login after successful registration
                    localStorage.setItem('authToken', data.token);
                    localStorage.setItem('userData', JSON.stringify({
                        id: data.id,
                        email: data.email,
                        firstName: data.firstName,
                        lastName: data.lastName,
                        role: data.role
                    }));

                    const expiryTime = Date.now() + (data.expiresIn || 86400000);
                    localStorage.setItem('tokenExpiry', expiryTime.toString());

                    showNotification('Registrierung erfolgreich! Willkommen bei Lebens Rhythmus!', 'success');

                    // Clear registration form
                    registerForm.reset();

                    // Redirect based on role
                    setTimeout(() => {
                        redirectBasedOnRole(data.role);
                    }, 2000);

                } else {
                    if (response.status === 409) {
                        showNotification('Diese E-Mail-Adresse ist bereits registriert.', 'error');
                    } else {
                        showNotification(data.message || 'Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut.', 'error');
                    }
                }

            } catch (error) {
                console.error('Registration error:', error);
                showNotification('Netzwerkfehler. Bitte überprüfen Sie Ihre Internetverbindung.', 'error');
            } finally {
                // Re-enable button
                const submitBtn = registerForm.querySelector('button[type="submit"]');
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
            }
        });
    }

    // ========== HELPER FUNCTIONS ==========

    /**
     * Check authentication status and redirect if already logged in
     */
    async function checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        const expiry = localStorage.getItem('tokenExpiry');

        if (token && expiry && Date.now() < parseInt(expiry)) {
            // Token exists and not expired
            try {
                // Verify token with backend
                const response = await fetch(ENDPOINTS.VERIFY_TOKEN || `${API_BASE_URL}/users/me`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.ok) {
                    // User is authenticated, redirect to dashboard
                    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
                    redirectBasedOnRole(userData.role || 'USER');
                } else {
                    // Token invalid, clear storage
                    clearAuthData();
                }
            } catch (error) {
                console.error('Token verification error:', error);
                // Keep user on login page if there's an error
            }
        } else {
            // Token expired or doesn't exist
            clearAuthData();
        }
    }

    /**
     * Clear authentication data from localStorage
     */
    function clearAuthData() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userData');
        localStorage.removeItem('tokenExpiry');
    }

    /**
     * Validate email format
     */
    function validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    /**
     * Show notification to user
     */
    function showNotification(message, type = 'info') {
        // Remove existing notifications
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) {
            existingNotification.remove();
        }

        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 25px;
            border-radius: 5px;
            color: white;
            font-weight: bold;
            z-index: 1000;
            animation: slideIn 0.3s ease;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        `;

        // Style based on type
        if (type === 'success') {
            notification.style.backgroundColor = '#4CAF50';
        } else if (type === 'error') {
            notification.style.backgroundColor = '#f44336';
        } else if (type === 'warning') {
            notification.style.backgroundColor = '#ff9800';
        } else {
            notification.style.backgroundColor = '#2196F3';
        }

        document.body.appendChild(notification);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    /**
     * Redirect user based on their role
     */
    function redirectBasedOnRole(role) {
        const basePath = window.location.origin + window.location.pathname.split('/pages/login')[0];

        switch(role) {
            case 'ADMIN':
                window.location.href = `${basePath}/pages/admin/dashboard.html`;
                break;
            case 'TEACHER':
                window.location.href = `${basePath}/pages/teacher/dashboard.html`;
                break;
            default: // USER
                window.location.href = `${basePath}/pages/dashboard/dashboard.html`;
        }
    }

    /**
     * Add password visibility toggle
     */
    function addPasswordVisibilityToggle() {
        const passwordInputs = document.querySelectorAll('input[type="password"]');

        passwordInputs.forEach(input => {
            const wrapper = document.createElement('div');
            wrapper.style.position = 'relative';
            wrapper.style.width = '100%';

            input.parentNode.insertBefore(wrapper, input);
            wrapper.appendChild(input);

            const toggleBtn = document.createElement('button');
            toggleBtn.type = 'button';
            toggleBtn.innerHTML = '👁️';
            toggleBtn.style.cssText = `
                position: absolute;
                right: 10px;
                top: 50%;
                transform: translateY(-50%);
                background: none;
                border: none;
                cursor: pointer;
                font-size: 16px;
                padding: 5px;
                z-index: 2;
            `;

            toggleBtn.addEventListener('click', function() {
                const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
                input.setAttribute('type', type);
                toggleBtn.innerHTML = type === 'password' ? '👁️' : '👁️‍🗨️';
            });

            wrapper.appendChild(toggleBtn);
        });
    }

    /**
     * Setup form validation with visual feedback
     */
    function setupFormValidation() {
        const inputs = document.querySelectorAll('input[required]');

        inputs.forEach(input => {
            input.addEventListener('blur', function() {
                if (this.value.trim() === '') {
                    this.style.borderColor = '#f44336';
                } else {
                    this.style.borderColor = '#4CAF50';
                }
            });

            input.addEventListener('input', function() {
                this.style.borderColor = '';
            });
        });
    }

    // ========== INITIALIZATION ==========

    // Add password visibility toggle
    addPasswordVisibilityToggle();

    // Setup form validation
    setupFormValidation();

    // Add CSS for animations
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        
        @keyframes slideOut {
            from { transform: translateX(0); opacity: 1; }
            to { transform: translateX(100%); opacity: 0; }
        }
        
        .login-container input:focus {
            border-color: #4CAF50;
            outline: none;
            box-shadow: 0 0 5px rgba(76, 175, 80, 0.5);
        }
        
        .gdpr-checkbox {
            margin: 15px 0;
            font-size: 0.9em;
        }
        
        .gdpr-checkbox a {
            color: #0066cc;
            text-decoration: none;
        }
        
        .gdpr-checkbox a:hover {
            text-decoration: underline;
        }
    `;
    document.head.appendChild(style);

    // ========== DEBUG / DEVELOPMENT HELPERS ==========

    // For development: Add test buttons
    if (window.location.hostname === 'localhost') {
        const debugDiv = document.createElement('div');
        debugDiv.style.cssText = `
            position: fixed;
            bottom: 10px;
            left: 10px;
            background: #f5f5f5;
            padding: 10px;
            border-radius: 5px;
            font-size: 12px;
            z-index: 9999;
        `;

        debugDiv.innerHTML = `
            <strong>Dev Mode</strong><br>
            API: ${API_BASE_URL}<br>
            <button onclick="localStorage.clear(); location.reload();">Clear Storage</button>
        `;

        document.body.appendChild(debugDiv);
    }
});

// ========== GLOBAL AUTH HELPER FUNCTIONS ==========

/**
 * Get authentication headers for API requests
 */
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

/**
 * Check if user is authenticated
 */
function isAuthenticated() {
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');

    if (!token || !expiry) return false;

    return Date.now() < parseInt(expiry);
}

/**
 * Logout function - clears auth data
 */
function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    localStorage.removeItem('tokenExpiry');
    window.location.href = '/pages/login/login.html';
}

// Make functions available globally
window.getAuthHeaders = getAuthHeaders;
window.isAuthenticated = isAuthenticated;
window.logout = logout;