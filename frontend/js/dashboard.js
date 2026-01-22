// frontend/js/dashboard.js
// Dashboard: show user name, load enrollments and optionally admin quick-actions.
// Uses a global window.API_BASE_URL and window.getAuthHeaders() / window.isAuthenticated()

document.addEventListener('DOMContentLoaded', async () => {
    // Redirect if not authenticated
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }

    // Load local userData (populated on login/registration)
    let localUser = {};
    try { localUser = JSON.parse(localStorage.getItem('userData') || '{}'); } catch (e) { console.warn('dashboard: bad userData', e); }

    // Display first name (fallback to email)
    const displayName = localUser.firstName || localUser.email || 'Gast';
    document.querySelectorAll('.userName').forEach(el => el.textContent = displayName);
    const userFirstNameEl = document.getElementById('userFirstName');
    if (userFirstNameEl) userFirstNameEl.textContent = displayName;

    // Show teacher/admin quick actions only for allowed roles
    const role = (localUser.role || '').toUpperCase();
    const allowed = ['ADMIN', 'TEACHER', 'BUSINESS_OWNER'];
    document.querySelectorAll('.teacher-only').forEach(section => {
        if (allowed.includes(role)) section.removeAttribute('hidden');
        else section.setAttribute('hidden', '');
    });

    // Try to refresh a profile from server (non-blocking)
    try {
        const profileRes = await fetch(`${window.API_BASE_URL}/users/me`, { headers: window.getAuthHeaders() });
        if (profileRes.ok) {
            const profile = await safeJson(profileRes);
            // update header if server has a newer firstName
            const name = profile.firstName || profile.email || displayName;
            document.querySelectorAll('.userName').forEach(el => el.textContent = name);
            // update stored userData minimally
            try {
                const raw = localStorage.getItem('userData');
                const ud = raw ? JSON.parse(raw) : {};
                ud.firstName = profile.firstName || ud.firstName;
                ud.lastName = profile.lastName || ud.lastName;
                ud.role = profile.role || ud.role;
                localStorage.setItem('userData', JSON.stringify(ud));
            } catch (e) { /* non-fatal */ }
        }
    } catch (err) {
        console.warn('dashboard: profile refresh failed', err);
    }

    // Load enrollments (Meine Kurse)
    try {
        const enrollRes = await fetch(`${window.API_BASE_URL}/users/me/enrollments`, { headers: window.getAuthHeaders() });
        if (!enrollRes.ok) throw new Error(`Enrollments fetch failed (${enrollRes.status})`);
        const enrollments = await safeJson(enrollRes);
        renderMyCourses(enrollments || []);
    } catch (err) {
        console.error('dashboard init error', err);
        const list = document.getElementById('myCoursesList');
        if (list) list.innerHTML = '<li>Fehler beim Laden</li>';
    }

    // If a user is admin — load simple stats
    if (role === 'ADMIN') {
        loadAdminStats().catch(e => console.warn('admin stats load failed', e));
    }
});

// Renders enrollment list to DOM
function renderMyCourses(enrollments) {
    const list = document.getElementById('myCoursesList');
    if (!list) return;
    if (!enrollments || enrollments.length === 0) {
        list.innerHTML = '<li>Keine Kurse</li>';
        return;
    }
    list.innerHTML = enrollments.map(e => `
      <li>
        <span class="course-title">${escapeHtml(e.workshopTitle || e.workshop?.title || 'Kurs')}</span>
        <div class="course-meta">
          <span class="status">${escapeHtml(e.status)}</span>
          <span class="date">${e.createdAt ? new Date(e.createdAt).toLocaleDateString('de-DE') : ''}</span>
        </div>
      </li>
    `).join('');
}

// Admin: fetch basic counts
async function loadAdminStats() {
    const statsBox = document.getElementById('adminStatsBox');
    if (!statsBox) return;
    try {
        const res = await fetch(`${window.API_BASE_URL}/users/stats/count`, { headers: window.getAuthHeaders() });
        if (!res.ok) {
            statsBox.innerHTML = '<div>Stats nicht verfügbar</div>';
            return;
        }
        const s = await safeJson(res);
        statsBox.innerHTML = `
            <div class="dash-card">
                <h3>Platform stats</h3>
                <ul>
                    <li>Total users: ${Number(s.totalUsers || 0)}</li>
                    <li>Active users: ${Number(s.activeUsers || 0)}</li>
                    <li>Users (role USER): ${Number(s.userCount || 0)}</li>
                    <li>Teachers: ${Number(s.teacherCount || 0)}</li>
                    <li>Admins: ${Number(s.adminCount || 0)}</li>
                </ul>
            </div>
        `;
    } catch (err) {
        console.warn('loadAdminStats error', err);
        statsBox.innerHTML = '<div>Stats load error</div>';
    }
}

// Utilities

// safe JSON parse helper: returns parsed json or {}/[] depending on content
async function safeJson(res) {
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const txt = await res.text().catch(() => '');
        if (!txt) return Array.isArray(txt) ? [] : {};
        try { return JSON.parse(txt); } catch { return {}; }
    }
    try { return await res.json(); } catch (err) { console.warn('safeJson parse failed', err); return {}; }
}

function escapeHtml(t){ const d=document.createElement('div'); d.textContent = t||''; return d.innerHTML; }