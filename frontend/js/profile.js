// profile.js
// Profile page logic: load profile, update profile, change password.
// Uses window.fetchJson and window.getAuthHeaders from main.js.
// All handlers include English comments explaining behavior.

document.addEventListener('DOMContentLoaded', initProfile);

function initProfile() {
    // If not authenticated — redirect to log in
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }

    // DOM references
    const profileForm = document.getElementById('profileForm');
    const passwordForm = document.getElementById('passwordForm');

    const firstNameEl = document.getElementById('firstName');
    const lastNameEl = document.getElementById('lastName');
    const emailEl = document.getElementById('email');
    const roleEl = document.getElementById('role');
    const phoneEl = document.getElementById('phone');
    const birthDateEl = document.getElementById('birthDate');

    const currentPasswordEl = document.getElementById('currentPassword');
    const newPasswordEl = document.getElementById('newPassword');
    const confirmPasswordEl = document.getElementById('confirmPassword');

    const notifications = ensureNotificationContainer();

    // load and populate profile on page load
    loadProfile();

    // ===== profile save =====
    profileForm && profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const firstName = firstNameEl.value.trim();
        const lastName = lastNameEl.value.trim();

        if (!firstName || firstName.length < 2) {
            showNotification('Vorname mindestens 2 Zeichen.', 'error');
            return;
        }
        if (!lastName || lastName.length < 2) {
            showNotification('Nachname mindestens 2 Zeichen.', 'error');
            return;
        }

        const payload = {
            firstName,
            lastName,
            phone: phoneEl.value.trim() || null,
            birthDate: birthDateEl.value || null
        };

        const btn = profileForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Speichern...';

        try {
            // Use fetchJson wrapper: throws on non-ok and handles 401
            const updated = await window.fetchJson(`${window.API_BASE_URL}/users/me`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            showNotification('Profil gespeichert.', 'success');

            // update localStorage userData so other pages reflect changes quickly
            try {
                const raw = localStorage.getItem('userData');
                const ud = raw ? JSON.parse(raw) : {};
                ud.firstName = updated.firstName || ud.firstName;
                ud.lastName = updated.lastName || ud.lastName;
                localStorage.setItem('userData', JSON.stringify(ud));
                // update header username if present
                document.querySelectorAll('.userName').forEach(el => el.textContent = ud.firstName || ud.email || 'Gast');
            } catch (err) {
                console.warn('profile: failed to update local userData', err);
            }
        } catch (err) {
            // fetchJson throws an Error with a message — show it
            console.error('profile save error', err);
            showNotification(err.message || 'Fehler beim Speichern', 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Speichern';
        }
    });

    // ===== change password =====
    passwordForm && passwordForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const current = currentPasswordEl.value;
        const next = newPasswordEl.value;
        const confirm = confirmPasswordEl.value;

        if (!current) {
            showNotification('Bitte aktuelles Passwort eingeben.', 'error');
            return;
        }
        if (!next || next.length < 6) {
            showNotification('Neues Passwort min. 6 Zeichen.', 'error');
            return;
        }
        if (next !== confirm) {
            showNotification('Neue Passwörter stimmen nicht überein.', 'error');
            return;
        }

        const btn = passwordForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Ändern...';

        try {
            // PUT /users/me/password — uses fetchJson: will throw on non-ok
            await window.fetchJson(`${window.API_BASE_URL}/users/me/password`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword: current, newPassword: next })
            });

            showNotification('Passwort erfolgreich geändert.', 'success');
            passwordForm.reset();
            // Optionally force logout — we keep the user logged in for MVP.
        } catch (err) {
            console.error('password change error', err);
            showNotification(err.message || 'Fehler beim Ändern des Passworts', 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Passwort ändern';
        }
    });

    // ===== helper: load profile =====
    async function loadProfile() {
        try {
            // GET /users/me via fetchJson (handles 401)
            const p = await window.fetchJson(`${window.API_BASE_URL}/users/me`, { method: 'GET' });
            populateProfile(p);
        } catch (err) {
            console.error('loadProfile error', err);
            showNotification(err.message || 'Fehler beim Laden des Profils.', 'error');
        }
    }

    // Populate form fields with profile DTO
    function populateProfile(p) {
        firstNameEl.value = p.firstName || '';
        lastNameEl.value = p.lastName || '';
        emailEl.value = p.email || '';
        roleEl.value = p.role || '';
        phoneEl.value = p.phone || '';
        birthDateEl.value = formatForInputDate(p.birthDate);
        // update header username if present
        document.querySelectorAll('.userName').forEach(el => el.textContent = p.firstName || p.email || 'Gast');
    }

    // ===== utils =====
    function formatForInputDate(d) {
        if (!d) return '';
        // accept 'YYYY-MM-DD' or ISO datetime
        if (typeof d === 'string' && d.length === 10 && /^\d{4}-\d{2}-\d{2}$/.test(d)) return d;
        try {
            const dt = new Date(d);
            if (isNaN(dt)) return '';
            const y = dt.getFullYear();
            const m = String(dt.getMonth() + 1).padStart(2, '0');
            const day = String(dt.getDate()).padStart(2, '0');
            return `${y}-${m}-${day}`;
        } catch {
            return '';
        }
    }

    function ensureNotificationContainer() {
        let c = document.getElementById('notificationContainer');
        if (c) return c;
        c = document.createElement('div');
        c.id = 'notificationContainer';
        c.style.position = 'fixed';
        c.style.top = '20px';
        c.style.right = '20px';
        c.style.zIndex = '9999';
        c.style.width = '320px';
        document.body.appendChild(c);
        return c;
    }

    function showNotification(msg, type = 'info') {
        const n = document.createElement('div');
        n.textContent = msg;
        n.style.padding = '10px 14px';
        n.style.marginBottom = '10px';
        n.style.borderRadius = '6px';
        n.style.color = '#fff';
        n.style.boxShadow = '0 2px 6px rgba(0,0,0,0.15)';
        n.style.opacity = '0.98';
        n.style.fontSize = '0.95rem';
        if (type === 'success') n.style.backgroundColor = '#4CAF50';
        else if (type === 'error') n.style.backgroundColor = '#f44336';
        else n.style.backgroundColor = '#2196F3';
        notifications.appendChild(n);
        setTimeout(() => {
            n.style.opacity = '0';
            setTimeout(() => n.remove(), 400);
        }, 3000);
    }
}
