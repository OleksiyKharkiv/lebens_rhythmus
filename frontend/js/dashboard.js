// dashboard.js
// Load user summary and enrollments for the Dashboard page.
// Uses window.fetchJson and window.getAuthHeaders from main.js.
// Comments explain flows and failure handling.

document.addEventListener('DOMContentLoaded', async () => {
    // If not authenticated — redirect to log in
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }

    // show name quickly from localStorage if present (optimistic)
    try {
        const raw = localStorage.getItem('userData');
        if (raw) {
            const ud = JSON.parse(raw);
            const nameEl = document.querySelectorAll('.userName');
            nameEl.forEach(e => e.textContent = ud.firstName || ud.email || 'Gast');
            // also reveal teacher/admin quick actions based on a role
            revealQuickActions(ud.role);
        }
    } catch (e) {
        // ignore parse errors and continue to fetch authoritative profile
        console.warn('dashboard: failed to read userData from localStorage', e);
    }

    // Fetch authoritative profile and enrollments
    try {
        // fetch profile from the backend (refreshes local display if necessary)
        const profile = await window.fetchJson(`${window.API_BASE_URL}/users/me`, { method: 'GET' });
        if (profile) {
            document.querySelectorAll('.userName').forEach(e => e.textContent = profile.firstName || profile.email || 'Gast');
            // store/refresh minimal userData in localStorage for other pages
            try {
                const ud = {
                    id: profile.id,
                    email: profile.email,
                    firstName: profile.firstName,
                    lastName: profile.lastName,
                    role: profile.role
                };
                localStorage.setItem('userData', JSON.stringify(ud));
                revealQuickActions(profile.role);
            } catch (e) { /* non-fatal */ }
        }

        // fetch enrollments for "Meine Kurse"
        const enrollments = await window.fetchJson(`${window.API_BASE_URL}/users/me/enrollments`, { method: 'GET' });
        renderMyCourses(enrollments || []);
    } catch (err) {
        console.error('dashboard init error', err);
        const list = document.getElementById('myCoursesList');
        if (list) list.innerHTML = '<li>Fehler beim Laden</li>';
    }
});

/**
 * Render enrollments into the "Meine Kurse" list.
 * Expected enrollment shape: { workshopTitle, status, createdAt }
 */
function renderMyCourses(enrollments) {
    const list = document.getElementById('myCoursesList');
    if (!list) return;

    if (!enrollments || enrollments.length === 0) {
        list.innerHTML = '<li>Keine Kurse</li>';
        return;
    }

    list.innerHTML = enrollments.map(e => `
      <li>
        <span class="course-title">${escapeHtml(e.workshopTitle || e.workshopName || 'Untitled')}</span>
        <div class="course-meta">
          <span class="status">${escapeHtml(e.status || '')}</span>
          <span class="date">${e.createdAt ? new Date(e.createdAt).toLocaleDateString('de-DE') : ''}</span>
        </div>
      </li>
    `).join('');
}

/**
 * Reveal teacher/admin quick actions block when a role matches.
 */
function revealQuickActions(role) {
    const block = document.querySelector('.teacher-only');
    if (!block) return;
    const allowed = ['TEACHER', 'ADMIN', 'BUSINESS_OWNER'];
    if (role && allowed.includes(role)) {
        block.hidden = false;
    } else {
        block.hidden = true;
    }
}

/** small util: escape text for HTML injection safety */
function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t || '';
    return d.innerHTML;
}