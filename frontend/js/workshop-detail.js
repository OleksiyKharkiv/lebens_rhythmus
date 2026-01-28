// frontend/js/workshop-detail.js
// Load workshop detail, groups and perform enrollment (protected).
// Uses window.API_BASE_URL and window.getAuthHeaders()

document.addEventListener('DOMContentLoaded', init);

async function init() {
    const params = new URLSearchParams(location.search);
    const id = params.get('id');
    if (!id) {
        const el = document.getElementById('workshop-detail');
        if (el) el.innerHTML = '<p>Workshop ID fehlt</p>';
        return;
    }
    await loadWorkshop(id);
}

async function loadWorkshop(id) {
    const base = window.API_BASE_URL;
    try {
        const res = await fetch(`${base}/workshops/${id}`);
        if (!res.ok) {
            const text = await safeText(res);
            console.error('Failed to load workshop', res.status, text);
            throw new Error('Failed to load workshop');
        }
        const w = await safeJsonOrObj(res);
        renderWorkshop(w);
    } catch (err) {
        console.error(err);
        const el = document.getElementById('workshop-detail');
        if (el) el.innerHTML = '<p>Fehler beim Laden</p>';
    }
}

function renderWorkshop(w) {
    document.getElementById('w-title').textContent = w.title;
    document.getElementById('w-desc').textContent = w.description || '';
    document.getElementById('w-meta').innerHTML = `
    <p><strong>Start:</strong> ${formatLocalDate(w.startDate)}</p>
    <p><strong>End:</strong> ${formatLocalDate(w.endDate)}</p>
    <p><strong>Ort:</strong> ${escapeHtml(w.venueName || 'TBA')}</p>
    <p><strong>Preis:</strong> ${w.price != null ? formatPrice(w.price) : 'auf Anfrage'}</p>
    <div class="workshop-actions" style="margin-top: 1rem;">
      <button class="btn primary" onclick="location.href='workshops.html?register=${w.id}#workshop-registration'">Anmelden</button>
    </div>
  `;

    const groupsEl = document.getElementById('groups-list');
    groupsEl.innerHTML = '';
    if (!w.groups || w.groups.length === 0) {
        groupsEl.innerHTML = '<p>Keine Gruppen verfügbar.</p>';
        return;
    }

    groupsEl.innerHTML = w.groups.map(g => `
    <div class="group-card" data-group-id="${g.id}">
      <h4>${escapeHtml(g.name || g.titleEn || 'Gruppe')}</h4>
      <p>${formatLocalDateTime(g.startDateTime)} — ${formatLocalDateTime(g.endDateTime || g.startDateTime)}</p>
      <p>Plätze: ${g.enrolledCount}/${g.capacity}</p>
      <button class="btn primary" onclick="enroll(${w.id}, ${g.id})" ${g.enrolledCount >= g.capacity ? 'disabled' : ''}>
        ${isAuthenticated() ? 'Anmelden' : 'Login zum Anmelden'}
      </button>
    </div>
  `).join('');
}

async function enroll(workshopId, groupId) {
    if (!isAuthenticated()) {
        // redirect back to detail after login
        location.href = `../login/login.html?redirect=workshop-detail.html?id=${workshopId}`;
        return;
    }
    const el = document.getElementById('enroll-result');
    el.textContent = 'Sende Anmeldung...';

    try {
        // Protected POST; ensure Content-Type and Authorization header present
        const res = await fetch(`${window.API_BASE_URL}/workshops/${workshopId}/enroll`, {
            method: 'POST',
            headers: Object.assign({}, window.getAuthHeaders(), { 'Content-Type': 'application/json' }),
            body: JSON.stringify({ groupId })
        });

        // read safely
        const text = await safeText(res);
        let data = {};
        if (text) {
            try { data = JSON.parse(text); } catch { data = { message: text }; }
        }

        if (!res.ok) {
            // show useful error message
            const msg = data?.message || `HTTP ${res.status}`;
            throw new Error(msg);
        }

        el.innerHTML = `<div class="success">Anmeldung erfolgreich (status: ${escapeHtml(data.status || 'CONFIRMED')}).</div>`;
        // reload to update counts
        await loadWorkshop(workshopId);
    } catch (err) {
        console.error(err);
        el.innerHTML = `<div class="error">${escapeHtml(err.message || 'Fehler')}</div>`;
    }
}

// helpers
function formatLocalDate(d) { if (!d) return ''; return new Date(d).toLocaleDateString('de-DE'); }
function formatLocalDateTime(d) { if (!d) return ''; return new Date(d).toLocaleString('de-DE', { dateStyle:'medium', timeStyle:'short' }); }
function formatPrice(p) { return p === 0 ? 'kostenlos' : new Intl.NumberFormat('de-DE', { style:'currency', currency:'EUR' }).format(p); }
function escapeHtml(t) { const div = document.createElement('div'); div.textContent = t || ''; return div.innerHTML; }

/* Safe response helpers (prevent JSON.parse on empty body) */
async function safeText(res) {
    try { return await res.text(); } catch { return ''; }
}
async function safeJsonOrObj(res) {
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const t = await safeText(res);
        if (!t) return {};
        try { return JSON.parse(t); } catch { return {}; }
    }
    try { return await res.json(); } catch (err) { console.warn('safeJsonOrObj parse failed', err); return {}; }
}