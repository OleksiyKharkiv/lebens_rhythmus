// frontend/js/admin-dashboard.js
// Admin dashboard script: ensures auth+role and loads simple recent actions + stats
// Uses window.API_BASE_URL and window.getAuthHeaders()

document.addEventListener('DOMContentLoaded', async () => {
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '/pages/login/login.html';
        return;
    }

    // require admin/teacher/business owner
    let user = {};
    try { user = JSON.parse(localStorage.getItem('userData') || '{}'); } catch {}
    const role = (user.role || '').toUpperCase();
    const allowed = ['ADMIN', 'TEACHER', 'BUSINESS_OWNER'];
    if (!allowed.includes(role)) {
        document.body.innerHTML = '<main style="padding:2rem"><h2>Unauthorized</h2><p>Du hast keine Rechte für diese Seite.</p></main>';
        return;
    }

    // show simple recent actions and link placeholders
    const recent = document.getElementById('recentActions');
    recent.innerHTML = '<p>Loading recent actions...</p>';

    try {
        // example: fetch last 5 workshops (public endpoint)
        const res = await fetch(`${window.API_BASE_URL}/workshops?upcoming=true`);
        const workshops = res.ok ? await safeJson(res) : [];
        if (!workshops || workshops.length === 0) {
            recent.innerHTML = '<p>No recent workshops</p>';
        } else {
            recent.innerHTML = workshops.slice(0,5).map(w => `<div style="padding:6px 0;border-bottom:1px solid #eee;"><strong>${escapeHtml(w.title)}</strong><br/><small>${escapeHtml(w.startDate || '')}</small></div>`).join('');
        }
    } catch (err) {
        console.warn('admin recent load failed', err);
        recent.innerHTML = '<p>Failed to load recent items</p>';
    }

    // admin stats are loaded by dashboard.js's loadAdminStats if that script is also present on the page
    // but call local stats fallback here if adminStatsBox empty
    try {
        const statsBox = document.getElementById('adminStatsBox');
        if (statsBox && statsBox.innerHTML.trim().toLowerCase().includes('loading')) {
            // attempt to fetch stats
            const r = await fetch(`${window.API_BASE_URL}/users/stats/count`, { headers: window.getAuthHeaders() });
            if (r.ok) {
                const s = await safeJson(r);
                statsBox.innerHTML = `<div class="dash-card"><h3>Platform stats</h3>
                    <ul>
                      <li>Total users: ${Number(s.totalUsers || 0)}</li>
                      <li>Active users: ${Number(s.activeUsers || 0)}</li>
                    </ul></div>`;
            } else {
                statsBox.innerHTML = '<div class="dash-card"><h3>Stats not available</h3></div>';
            }
        }
    } catch (err) {
        console.warn('admin stats error', err);
    }
});

// helpers (re-used)
async function safeJson(res) {
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const t = await res.text().catch(()=>'');
        if (!t) return {};
        try { return JSON.parse(t); } catch { return {}; }
    }
    try { return await res.json(); } catch { return {}; }
}
function escapeHtml(t){ const d=document.createElement('div'); d.textContent = t||''; return d.innerHTML; }