/**
 * Login/Registration Page Script for Lebens Rhythmus
 * Интеграция с Spring Boot бэкендом с использованием JWT аутентификации
 */

document.addEventListener('DOMContentLoaded', function() {
    // ========== КОНФИГУРАЦИЯ ==========
    const API_BASE_URL = window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api'
        : 'https://api.tlab29.com/api';

    const ENDPOINTS = {
        LOGIN: `${API_BASE_URL}/auth/login`,
        REGISTER: `${API_BASE_URL}/auth/register`,
        VERIFY_TOKEN: `${API_BASE_URL}/auth/verify-token`
    };

    // ========== ДОМ ЭЛЕМЕНТЫ ==========
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    // Элементы формы логина
    const loginEmail = document.getElementById('loginEmail');
    const loginPassword = document.getElementById('loginPassword');

    // Элементы формы регистрации
    const registerFirstName = document.getElementById('registerFirstName');
    const registerLastName = document.getElementById('registerLastName');
    const registerEmail = document.getElementById('registerEmail');
    const registerPassword = document.getElementById('registerPassword');
    const registerConfirmPassword = document.getElementById('registerConfirmPassword');
    const acceptTerms = document.getElementById('acceptTerms');
    const acceptPrivacy = document.getElementById('acceptPrivacy');

    // ========== ПРОВЕРКА СТАТУСА АУТЕНТИФИКАЦИИ ==========
    checkAuthStatus();

    // ========== ОБРАБОТЧИК ФОРМЫ ЛОГИНА ==========
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const email = loginEmail.value.trim();
            const password = loginPassword.value;

            // Валидация на фронтенде
            if (!validateEmail(email)) {
                showNotification('Bitte geben Sie eine gültige E-Mail-Adresse ein.', 'error');
                highlightField(loginEmail, 'error');
                return;
            }

            if (password.length < 6) {
                showNotification('Passwort muss mindestens 6 Zeichen lang sein.', 'error');
                highlightField(loginPassword, 'error');
                return;
            }

            try {
                // Отключаем кнопку во время запроса
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.textContent;
                submitBtn.textContent = 'Wird eingeloggt...';
                submitBtn.disabled = true;

                // Подготавливаем запрос согласно структуре UserLoginRequestDTO
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
                    // Сохраняем токен и данные пользователя согласно UserLoginResponseDTO
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

                    // Сохраняем время истечения токена
                    const expiryTime = Date.now() + (data.expiresIn || 86400000);
                    localStorage.setItem('tokenExpiry', expiryTime.toString());

                    showNotification('Erfolgreich eingeloggt! Willkommen zurück!', 'success');

                    // Перенаправляем на dashboard в зависимости от роли
                    setTimeout(() => {
                        redirectBasedOnRole(data.role);
                    }, 1500);

                } else {
                    // Обрабатываем конкретные случаи ошибок
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
                // Включаем кнопку обратно
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.textContent = 'Login';
                    submitBtn.disabled = false;
                }
            }
        });
    }

    // ========== ОБРАБОТЧИК ФОРМЫ РЕГИСТРАЦИИ ==========
    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const firstName = registerFirstName.value.trim();
            const lastName = registerLastName.value.trim();
            const email = registerEmail.value.trim();
            const password = registerPassword.value;
            const confirmPassword = registerConfirmPassword.value;

            // Валидация
            let isValid = true;

            if (!firstName || firstName.length < 2) {
                showNotification('Bitte geben Sie einen gültigen Vornamen ein (mindestens 2 Zeichen).', 'error');
                highlightField(registerFirstName, 'error');
                isValid = false;
            } else {
                highlightField(registerFirstName, 'success');
            }

            if (!lastName || lastName.length < 2) {
                showNotification('Bitte geben Sie einen gültigen Nachnamen ein (mindestens 2 Zeichen).', 'error');
                highlightField(registerLastName, 'error');
                isValid = false;
            } else {
                highlightField(registerLastName, 'success');
            }

            if (!validateEmail(email)) {
                showNotification('Bitte geben Sie eine gültige E-Mail-Adresse ein.', 'error');
                highlightField(registerEmail, 'error');
                isValid = false;
            } else {
                highlightField(registerEmail, 'success');
            }

            if (password.length < 6) {
                showNotification('Passwort muss mindestens 6 Zeichen lang sein.', 'error');
                highlightField(registerPassword, 'error');
                isValid = false;
            } else {
                highlightField(registerPassword, 'success');
            }

            if (password !== confirmPassword) {
                showNotification('Passwörter stimmen nicht überein.', 'error');
                highlightField(registerConfirmPassword, 'error');
                isValid = false;
            } else if (password.length >= 6) {
                highlightField(registerConfirmPassword, 'success');
            }

            if (!acceptTerms.checked) {
                showNotification('Bitte akzeptieren Sie die Allgemeinen Geschäftsbedingungen.', 'error');
                isValid = false;
            }

            if (!acceptPrivacy.checked) {
                showNotification('Bitte akzeptieren Sie die Datenschutzbestimmungen.', 'error');
                isValid = false;
            }

            if (!isValid) return;

            try {
                // Отключаем кнопку во время запроса
                const submitBtn = registerForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.textContent;
                submitBtn.textContent = 'Wird registriert...';
                submitBtn.disabled = true;

                // Подготавливаем запрос согласно структуре UserRegistrationDTO
                const registrationData = {
                    email: email,
                    password: password,
                    firstName: firstName,
                    lastName: lastName,
                    phone: '', // Необязательное поле
                    birthDate: null, // Необязательное поле
                    role: 'USER', // Роль по умолчанию
                    address: '', // Необязательное
                    city: '', // Необязательное
                    zipCode: '', // Необязательное
                    country: 'Deutschland', // По умолчанию
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
                    // Автоматический логин после успешной регистрации
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

                    // Очищаем форму регистрации
                    registerForm.reset();

                    // Перенаправляем в зависимости от роли
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
                // Включаем кнопку обратно
                const submitBtn = registerForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.textContent = 'Registrieren';
                    submitBtn.disabled = false;
                }
            }
        });
    }

    // ========== ИНИЦИАЛИЗАЦИЯ ДОПОЛНИТЕЛЬНЫХ ФУНКЦИЙ ==========

    // Инициализация переключателей видимости пароля
    initPasswordToggles();

    // Настройка валидации форм в реальном времени
    setupRealTimeValidation();

    // ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

    /**
     * Проверка статуса аутентификации и перенаправление, если уже авторизован
     */
    async function checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        const expiry = localStorage.getItem('tokenExpiry');

        if (token && expiry && Date.now() < parseInt(expiry)) {
            // Токен существует и не истек
            try {
                // Проверяем токен с бэкендом
                const response = await fetch(`${API_BASE_URL}/users/me`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.ok) {
                    // Пользователь авторизован, перенаправляем на dashboard
                    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
                    redirectBasedOnRole(userData.role || 'USER');
                } else {
                    // Токен недействителен, очищаем хранилище
                    clearAuthData();
                }
            } catch (error) {
                console.error('Token verification error:', error);
                // Оставляем пользователя на странице логина при ошибке
            }
        } else {
            // Токен истек или не существует
            clearAuthData();
        }
    }

    /**
     * Очистка данных аутентификации из localStorage
     */
    function clearAuthData() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userData');
        localStorage.removeItem('tokenExpiry');
    }

    /**
     * Валидация формата email
     */
    function validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    }

    /**
     * Показать уведомление пользователю
     */
    function showNotification(message, type = 'info') {
        // Удаляем существующие уведомления
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) {
            existingNotification.remove();
        }

        // Создаем элемент уведомления
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;

        document.body.appendChild(notification);

        // Автоматическое удаление через 5 секунд
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOut 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    /**
     * Подсветить поле ввода
     */
    function highlightField(field, type) {
        field.classList.remove('error', 'success');
        if (type === 'error' || type === 'success') {
            field.classList.add(type);
        }
    }

    /**
     * Перенаправить пользователя в зависимости от его роли
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
     * Инициализация переключателей видимости пароля
     */
    function initPasswordToggles() {
        const toggleButtons = document.querySelectorAll('.toggle-password');

        toggleButtons.forEach(button => {
            button.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();

                const targetId = this.getAttribute('data-target');
                const passwordInput = document.getElementById(targetId);

                if (passwordInput) {
                    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                    passwordInput.setAttribute('type', type);

                    // Меняем иконку
                    this.textContent = type === 'password' ? '👁️' : '👁️‍🗨️';

                    // Фокусируемся на поле ввода
                    setTimeout(() => {
                        passwordInput.focus();
                        // Помещаем курсор в конец текста
                        const length = passwordInput.value.length;
                        passwordInput.setSelectionRange(length, length);
                    }, 10);
                }
            });

            // Предотвращаем фокус на кнопке при табуляции
            button.setAttribute('tabindex', '-1');
        });
    }

    /**
     * Настройка валидации в реальном времени
     */
    function setupRealTimeValidation() {
        // Валидация email в реальном времени
        if (loginEmail) {
            loginEmail.addEventListener('blur', function() {
                if (this.value.trim() && !validateEmail(this.value.trim())) {
                    highlightField(this, 'error');
                } else if (this.value.trim()) {
                    highlightField(this, 'success');
                }
            });
        }

        if (registerEmail) {
            registerEmail.addEventListener('blur', function() {
                if (this.value.trim() && !validateEmail(this.value.trim())) {
                    highlightField(this, 'error');
                } else if (this.value.trim()) {
                    highlightField(this, 'success');
                }
            });
        }

        // Валидация пароля в реальном времени
        if (loginPassword) {
            loginPassword.addEventListener('blur', function() {
                if (this.value.length > 0 && this.value.length < 6) {
                    highlightField(this, 'error');
                } else if (this.value.length >= 6) {
                    highlightField(this, 'success');
                }
            });
        }

        if (registerPassword) {
            registerPassword.addEventListener('blur', function() {
                if (this.value.length > 0 && this.value.length < 6) {
                    highlightField(this, 'error');
                } else if (this.value.length >= 6) {
                    highlightField(this, 'success');
                }
            });
        }

        // Валидация подтверждения пароля в реальном времени
        if (registerPassword && registerConfirmPassword) {
            registerConfirmPassword.addEventListener('input', function() {
                if (this.value !== registerPassword.value && this.value.length > 0) {
                    highlightField(this, 'error');
                } else if (this.value === registerPassword.value && this.value.length > 0) {
                    highlightField(this, 'success');
                }
            });

            registerPassword.addEventListener('input', function() {
                if (registerConfirmPassword.value && this.value !== registerConfirmPassword.value) {
                    highlightField(registerConfirmPassword, 'error');
                } else if (registerConfirmPassword.value && this.value === registerConfirmPassword.value) {
                    highlightField(registerConfirmPassword, 'success');
                }
            });
        }
    }
});

// ========== ГЛОБАЛЬНЫЕ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ДЛЯ АУТЕНТИФИКАЦИИ ==========

/**
 * Получить заголовки аутентификации для API запросов
 */
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

/**
 * Проверить, авторизован ли пользователь
 */
function isAuthenticated() {
    const token = localStorage.getItem('authToken');
    const expiry = localStorage.getItem('tokenExpiry');

    if (!token || !expiry) return false;

    return Date.now() < parseInt(expiry);
}

/**
 * Функция выхода - очищает данные аутентификации
 */
function logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userData');
    localStorage.removeItem('tokenExpiry');
    window.location.href = '/pages/login/login.html';
}

// Делаем функции доступными глобально
window.getAuthHeaders = getAuthHeaders;
window.isAuthenticated = isAuthenticated;
window.logout = logout;