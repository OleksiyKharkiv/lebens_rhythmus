// frontend/js/workshop-detail.js
document.addEventListener('DOMContentLoaded', init);

async function init() {
    const params = new URLSearchParams(location.search);
    const id = params.get('id');
    if (!id) {
        document.getElementById('workshop-detail').innerHTML = '<p>Workshop ID fehlt</p>'; return;
    }
    await loadWorkshop(id);
}

async function loadWorkshop(id) {
    const base = window.API_BASE_URL;
    try {
        const res = await fetch(`${base}/workshops/${id}`);
        if (!res.ok) throw new Error('Failed to load workshop');
        const w = await res.json();
        renderWorkshop(w);
    } catch (err) {
        console.error(err);
        document.getElementById('workshop-detail').innerHTML = '<p>Fehler beim Laden</p>';
    }
}

function renderWorkshop(w) {
    document.getElementById('w-title').textContent = w.title;
    document.getElementById('w-desc').textContent = w.description || '';
    document.getElementById('w-meta').innerHTML = `
    <p><strong>Start:</strong> ${formatLocalDate(w.startDate)}</p>
    <p><strong>End:</strong> ${formatLocalDate(w.endDate)}</p>
    <p><strong>Ort:</strong> ${w.venueName || 'TBA'}</p>
    <p><strong>Preis:</strong> ${w.price != null ? formatPrice(w.price) : 'auf Anfrage'}</p>
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
        location.href = '../login/login.html?redirect=workshop-detail.html?id=' + workshopId;
        return;
    }
    const el = document.getElementById('enroll-result');
    el.textContent = 'Sende Anmeldung...';

    try {
        const res = await fetch(`${window.API_BASE_URL}/workshops/${workshopId}/enroll`, {
            method: 'POST',
            headers: window.getAuthHeaders(),
            body: JSON.stringify({ groupId })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Enrollment failed');
        el.innerHTML = `<div class="success">Anmeldung erfolgreich (status: ${data.status}).</div>`;
        // reload groups to reflect new counts
        loadWorkshop(workshopId);
    } catch (err) {
        console.error(err);
        el.innerHTML = `<div class="error">${escapeHtml(err.message || 'Fehler')}</div>`;
    }
}

// helpers:
function formatLocalDate(d){ if(!d) return ''; return new Date(d).toLocaleDateString('de-DE'); }
function formatLocalDateTime(d){ if(!d) return ''; return new Date(d).toLocaleString('de-DE',{dateStyle:'medium',timeStyle:'short'}); }
function formatPrice(p){ return p===0?'kostenlos': new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'}).format(p); }
function escapeHtml(t){ const div=document.createElement('div'); div.textContent = t||''; return div.innerHTML; }
