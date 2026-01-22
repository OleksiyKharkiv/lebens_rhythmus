// frontend/js/workshops.js
// Loads public list of workshops, renders cards and populates registration <select>.
// Uses global window.API_BASE_URL and helper isAuthenticated()

document.addEventListener('DOMContentLoaded', () => {
    if (typeof window.API_BASE_URL === 'undefined') {
        console.error('API base not defined');
        return;
    }
    loadWorkshops();
});

async function loadWorkshops() {
    const container = document.getElementById('workshops-content');
    if (!container) return;
    container.innerHTML = '<p>Loading...</p>';

    try {
        // Public GET - no auth required
        const res = await fetch(`${window.API_BASE_URL}/workshops`);
        if (!res.ok) {
            const text = await safeText(res);
            console.error('Failed to fetch workshops', res.status, text);
            throw new Error('Failed to fetch workshops');
        }

        const workshops = await safeJsonOrEmpty(res);

        if (!workshops || workshops.length === 0) {
            container.innerHTML = '<p class="no-workshops">No upcoming workshops are available right now.</p>';
            populateWorkshopSelect([]); // ensure select cleared
            return;
        }

        // render cards
        container.innerHTML = workshops.map(w => renderWorkshopCard(w)).join('');

        // populate select for registration (only PUBLISHED)
        populateWorkshopSelect(workshops);
    } catch (err) {
        console.error(err);
        container.innerHTML = `<p class="error">Fehler beim Laden der Workshops.</p>`;
        populateWorkshopSelect([]); // clear select on error
    }
}

function renderWorkshopCard(w) {
    const start = w.startDate ? formatLocalDate(w.startDate) : 'TBA';
    const venue = w.venueName || 'TBA';
    const price = (w.price != null) ? formatPrice(w.price) : 'auf Anfrage';

    // If API returns currentParticipants & maxParticipants, show spots.
    let spotsInfo = '';
    if (typeof w.currentParticipants !== 'undefined' && typeof w.maxParticipants !== 'undefined') {
        const left = Math.max(0, w.maxParticipants - w.currentParticipants);
        spotsInfo = `<p><strong>Verfügbar:</strong> ${left}/${w.maxParticipants}</p>`;
    }

    const enrollText = isAuthenticated() ? 'Anmelden' : 'Login zum Anmelden';
    const enrollAction = isAuthenticated()
        ? `onclick="location.href='workshop-detail.html?id=${w.id}'"`
        : `onclick="location.href='../login/login.html?redirect=workshops'"`;

    return `
    <div class="workshop-item">
      <h3>${escapeHtml(w.title)}</h3>
      <p class="workshop-description">${escapeHtml(w.shortDescription || '')}</p>
      <div class="workshop-details">
        <p><strong>Start:</strong> ${escapeHtml(start)}</p>
        <p><strong>Ort:</strong> ${escapeHtml(venue)}</p>
        <p><strong>Preis:</strong> ${escapeHtml(price)}</p>
        ${spotsInfo}
      </div>
      <div class="workshop-actions">
        <button class="btn" onclick="location.href='workshop-detail.html?id=${w.id}'">Details</button>
        <button class="btn primary" ${enrollAction}>${enrollText}</button>
      </div>
    </div>
  `;
}

/* ====== New: populate the <select id="workshop-select"> used in registration form ====== */
function populateWorkshopSelect(workshops) {
    const sel = document.getElementById('workshop-select');
    if (!sel) return;

    // keep the default placeholder option
    sel.innerHTML = '<option value="">Choose a workshop...</option>';

    // Filter: show only published workshops for registration
    const published = (workshops || []).filter(w => {
        const s = (w.status || '').toString().toUpperCase();
        return s === 'PUBLISHED';
    });

    // Sort by startDate ascending (nulls last)
    published.sort((a, b) => {
        if (!a.startDate && !b.startDate) return 0;
        if (!a.startDate) return 1;
        if (!b.startDate) return -1;
        return new Date(a.startDate) - new Date(b.startDate);
    });

    published.forEach(w => {
        const opt = document.createElement('option');
        opt.value = String(w.id);
        const dateLabel = w.startDate ? ` — ${formatLocalDate(w.startDate)}` : '';
        opt.textContent = `${w.title || 'Untitled'}${dateLabel}`;
        sel.appendChild(opt);
    });

    // If no published workshops, add disabled placeholder
    if (published.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'No workshops available';
        opt.disabled = true;
        sel.appendChild(opt);
    }
}

/* ===== helpers ===== */
function formatLocalDate(d) {
    try {
        const date = new Date(d);
        return date.toLocaleDateString('de-DE', {year: 'numeric', month: 'short', day: 'numeric'});
    } catch {
        return d;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
}

function formatPrice(p) {
    return p === 0 ? 'kostenlos' : new Intl.NumberFormat('de-DE', {style: 'currency', currency: 'EUR'}).format(p);
}

/* ======= Helpers: safe parsing to avoid JSON.parse errors on empty bodies ====== */
async function safeText(res) {
    try {
        return await res.text();
    } catch {
        return '';
    }
}

async function safeJsonOrEmpty(res) {
    // if no content (204 or empty) return []
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) {
        const t = await safeText(res);
        if (!t) return [];
        try {
            return JSON.parse(t);
        } catch {
            return [];
        }
    }
    try {
        return await res.json();
    } catch (err) {
        console.warn('safeJsonOrEmpty: parse failed', err);
        return [];
    }
}