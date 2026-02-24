// frontend/js/admin-dashboard.js
// Admin dashboard script: ensures auth+role and loads simple recent actions + stats
// Uses window.API_BASE_URL and window.getAuthHeaders()

document.addEventListener('DOMContentLoaded', async () => {
    // === Auth + role check ===
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        // not logged in -> back to the login
        location.href = '/pages/login/login.html';
        return;
    }

    let user = {};
    try {
        user = JSON.parse(localStorage.getItem('userData') || '{}');
    } catch (e) {
        user = {};
    }

    const role = (user.role || '').toUpperCase();
    const allowed = ['ADMIN', 'TEACHER', 'BUSINESS_OWNER'];
    if (!allowed.includes(role)) {
        // show a friendly unauthorized message
        document.body.innerHTML = '<main style="padding:2rem"><h2>Unauthorized</h2><p>Du hast keine Rechte für diese Seite.</p></main>';
        return;
    }

    // show/hide quick actions (elements with class .teacher-only)
    document.querySelectorAll('.teacher-only').forEach(el => {
        el.hidden = !allowed.includes(role);
    });

    // === Recent actions (workshops preview) ===
    const recent = document.getElementById('recentActions');
    if (recent) recent.innerHTML = '<p>Loading recent items…</p>';

    try {
        const workshops = await window.fetchJson(`${window.API_BASE_URL}/workshops?upcoming=true`);
        if (!workshops || workshops.length === 0) {
            if (recent) recent.innerHTML = '<p>No recent workshops</p>';
        } else {
            const items = workshops.slice(0, 5).map(w => {
                const date = w.startDate ? window.escapeHtml(window.formatLocalDate(w.startDate)) : '';
                return `<div style="padding:6px 0;border-bottom:1px solid #eee;">
                  <strong>${window.escapeHtml(w.title || w.workshopName || '—')}</strong><br/>
                  <small>${date}</small>
                </div>`;
            }).join('');
            if (recent) recent.innerHTML = items;
        }
    } catch (err) {
        console.warn('admin recent load failed', err);
        if (recent) recent.innerHTML = '<p>Failed to load recent items</p>';
    }

    // === Stats ===
    const statsBox = document.getElementById('adminStatsBox');
    if (statsBox) {
        statsBox.innerHTML = '<h3>Loading stats…</h3>';
        try {
            const s = await window.fetchJson(`${window.API_BASE_URL}/users/stats/count`);
            statsBox.innerHTML = `<div>
          <h3>Platform stats</h3>
          <ul>
            <li>Total users: ${Number(s.totalUsers || 0)}</li>
            <li>Active users: ${Number(s.activeUsers || 0)}</li>
            <li>Users: ${Number(s.userCount || 0)}</li>
            <li>Teachers: ${Number(s.teacherCount || 0)}</li>
            <li>Admins: ${Number(s.adminCount || 0)}</li>
          </ul>
        </div>`;
        } catch (err) {
            console.warn('admin stats error', err);
            statsBox.innerHTML = `<div><h3>Stats</h3><p>${err.status === 403 ? 'Access denied' : 'Error'}</p></div>`;
        }
    }
});

/* ---------------- helpers ---------------- */

// Thin wrapper around fetch that attaches Authorization header if available.
// Accepts the same args as fetch; returns raw Response (doesn't throw on non-ok).
async function safeFetch(url, opts = {}) {
    const final = Object.assign({}, opts);
    final.headers = Object.assign({}, final.headers || {}, window.getAuthHeaders ? window.getAuthHeaders() : {});
    return fetch(url, final);
}

async function safeJson(res) {
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const t = await res.text().catch(() => '');
        if (!t) return {};
        try {
            return JSON.parse(t);
        } catch {
            return {};
        }
    }
    try {
        return await res.json();
    } catch {
        return {};
    }
}