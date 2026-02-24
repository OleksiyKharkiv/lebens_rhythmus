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
    try {
        const w = await window.fetchJson(`${window.API_BASE_URL}/workshops/${id}`);
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
    <p><strong>Start:</strong> ${window.formatLocalDate(w.startDate)}</p>
    <p><strong>End:</strong> ${window.formatLocalDate(w.endDate)}</p>
    <p><strong>Ort:</strong> ${window.escapeHtml(w.venueName || 'TBA')}</p>
    <p><strong>Preis:</strong> ${w.price != null ? window.formatPrice(w.price) : 'auf Anfrage'}</p>
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
      <h4>${window.escapeHtml(g.name || g.titleEn || 'Gruppe')}</h4>
      <p>${formatLocalDateTime(g.startDateTime)} — ${formatLocalDateTime(g.endDateTime || g.startDateTime)}</p>
      <p>Plätze: ${g.enrolledCount}/${g.capacity}</p>
      <button class="btn primary" onclick="enroll(${w.id}, ${g.id})" ${g.enrolledCount >= g.capacity ? 'disabled' : ''}>
        ${window.isAuthenticated() ? 'Anmelden' : 'Login zum Anmelden'}
      </button>
    </div>
  `).join('');
}

async function enroll(workshopId, groupId) {
    if (!window.isAuthenticated()) {
        // redirect back to detail after login
        location.href = `../login/login.html?redirect=workshop-detail.html?id=${workshopId}`;
        return;
    }
    const el = document.getElementById('enroll-result');
    el.textContent = 'Sende Anmeldung...';

    try {
        const data = await window.fetchJson(`${window.API_BASE_URL}/workshops/${workshopId}/enroll`, {
            method: 'POST',
            body: JSON.stringify({groupId})
        });

        el.innerHTML = `<div class="success">Anmeldung erfolgreich (status: ${window.escapeHtml(data.status || 'CONFIRMED')}).</div>`;
        // reload to update counts
        await loadWorkshop(workshopId);
    } catch (err) {
        console.error(err);
        el.innerHTML = `<div class="error">${window.escapeHtml(err.message || 'Fehler')}</div>`;
    }
}

// helpers
function formatLocalDateTime(d) {
    if (!d) return '';
    return new Date(d).toLocaleString('de-DE', {dateStyle: 'medium', timeStyle: 'short'});
}