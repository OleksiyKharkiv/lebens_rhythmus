// frontend/js/workshops.js
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
        const res = await fetch(`${window.API_BASE_URL}/workshops`);
        if (!res.ok) throw new Error('Failed to fetch workshops');
        const workshops = await res.json();

        if (!workshops || workshops.length === 0) {
            container.innerHTML = '<p class="no-workshops">No upcoming workshops are available right now.</p>';
            return;
        }

        container.innerHTML = workshops.map(w => renderWorkshopCard(w)).join('');
    } catch (err) {
        console.error(err);
        container.innerHTML = `<p class="error">Fehler beim Laden der Workshops.</p>`;
    }
}

function renderWorkshopCard(w) {
    const start = w.startDate ? formatLocalDate(w.startDate) : 'TBA';
    const venue = w.venueName || 'TBA';
    const price = w.price != null ? formatPrice(w.price) : 'auf Anfrage';
    const enrollText = isAuthenticated() ? 'Anmelden' : 'Login zum Anmelden';
    const enrollAction = isAuthenticated()
        ? `onclick="location.href='workshop-detail.html?id=${w.id}'"`
        : `onclick="location.href='../login/login.html?redirect=workshops'"`
    ;

    return `
    <div class="workshop-item">
      <h3>${escapeHtml(w.title)}</h3>
      <p class="workshop-description">${escapeHtml(w.shortDescription || '')}</p>
      <div class="workshop-details">
        <p><strong>Start:</strong> ${escapeHtml(start)}</p>
        <p><strong>Ort:</strong> ${escapeHtml(venue)}</p>
        <p><strong>Preis:</strong> ${escapeHtml(price)}</p>
      </div>
      <div class="workshop-actions">
        <button class="btn" onclick="location.href='workshop-detail.html?id=${w.id}'">Details</button>
        <button class="btn primary" ${enrollAction}>${enrollText}</button>
      </div>
    </div>
  `;
}

function formatLocalDate(d) {
    try {
        const date = new Date(d);
        return date.toLocaleDateString('de-DE', { year:'numeric', month:'short', day:'numeric' });
    } catch { return d; }
}

function escapeHtml(text){ const div=document.createElement('div'); div.textContent = text || ''; return div.innerHTML; }
function formatPrice(p){ return p === 0 ? 'kostenlos' : new Intl.NumberFormat('de-DE',{style:'currency',currency:'EUR'}).format(p); }
