// frontend/js/workshops.js
// Loads a public list of workshops, renders cards and populates registration <select>.
// Uses a global window.API_BASE_URL and helper isAuthenticated()

document.addEventListener('DOMContentLoaded', () => {
    if (typeof window.API_BASE_URL === 'undefined') {
        console.error('API base not defined');
        return;
    }
    loadWorkshops();

    // Add form submission handler
    const form = document.getElementById('workshop-registration-form');
    if (form) {
        form.addEventListener('submit', handleRegistration);
    }
});

async function handleRegistration(e) {
    e.preventDefault();
    if (!window.isAuthenticated()) {
        const currentPath = window.location.pathname;
        const page = currentPath.substring(currentPath.lastIndexOf('/') + 1);
        location.href = `../login/login.html?redirect=${page}`;
        return;
    }

    const form = e.target;
    const workshopId = form.workshopId.value;
    if (!workshopId) {
        alert('Bitte wählen Sie einen Workshop aus.');
        return;
    }

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.disabled = true;

    try {
        await window.fetchJson(`${window.API_BASE_URL}/workshops/${workshopId}/enroll`, {
            method: 'POST',
            body: JSON.stringify({})
        });

        alert('Anmeldung erfolgreich!');
        form.reset();
    } catch (err) {
        console.error(err);
        alert('Fehler: ' + (err.message || 'Anmeldung fehlgeschlagen'));
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

window.scrollToRegistration = function (id) {
    const sel = document.getElementById('workshop-select');
    if (sel) {
        sel.value = String(id);
    }
    const section = document.querySelector('.workshop-registration');
    if (section) {
        section.scrollIntoView({behavior: 'smooth'});
    }
};

async function loadWorkshops() {
    const container = document.getElementById('workshops-content');
    if (!container) return;
    container.innerHTML = '<p>Loading...</p>';

    try {
        const workshops = await window.fetchJson(`${window.API_BASE_URL}/workshops`);

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
    const start = w.startDate ? window.formatLocalDate(w.startDate) : 'TBA';
    const venue = w.venueName || 'TBA';
    const price = (w.price != null) ? window.formatPrice(w.price) : 'auf Anfrage';

    // decide details URL so it works from admin pages and from public pages
    function detailHrefFor(id) {
        const p = window.location.pathname || '';
        // if we're inside an "admin" path, go up one level to workshops folder
        if (p.includes('/admin/')) {
            return `../workshops/workshop-detail.html?id=${id}`;
        }
        // if already inside workshops folder
        if (p.includes('/workshops/')) {
            return `workshop-detail.html?id=${id}`;
        }
        // fallback to site-root /workshops (works on prod)
        return `/workshops/workshop-detail.html?id=${id}`;
    }

    const detailHref = detailHrefFor(w.id);

    const enrollText = window.isAuthenticated() ? 'Anmelden' : 'Login zum Anmelden';
    const enrollAction = window.isAuthenticated()
        ? `onclick="scrollToRegistration(${w.id})"`
        : `onclick="location.href='../login/login.html?redirect=workshops'"`;

    // If API returns currentParticipants & maxParticipants, show spots.
    let spotsInfo = '';
    if (typeof w.currentParticipants !== 'undefined' && typeof w.maxParticipants !== 'undefined') {
        const left = Math.max(0, w.maxParticipants - w.currentParticipants);
        spotsInfo = `<p><strong>Verfügbar:</strong> ${left}/${w.maxParticipants}</p>`;
    }

    return `
    <div class="workshop-item">
      <h3>${window.escapeHtml(w.title)}</h3>
      <p class="workshop-description">${window.escapeHtml(w.shortDescription || '')}</p>
      <div class="workshop-details">
        <p><strong>Start:</strong> ${window.escapeHtml(start)}</p>
        <p><strong>Ort:</strong> ${window.escapeHtml(venue)}</p>
        <p><strong>Preis:</strong> ${window.escapeHtml(price)}</p>
        ${spotsInfo}
      </div>
      <div class="workshop-actions">
        <button class="btn" onclick="location.href='${detailHref}'">Details</button>
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

    // Filter: show workshops that are not ARCHIVED or CANCELLED
    const available = (workshops || []).filter(w => {
        const s = (w.status || '').toString().toUpperCase();
        return s !== 'ARCHIVED' && s !== 'CANCELLED';
    });

    // Sort by startDate ascending (nulls last)
    available.sort((a, b) => {
        if (!a.startDate && !b.startDate) return 0;
        if (!a.startDate) return 1;
        if (!b.startDate) return -1;
        return new Date(a.startDate) - new Date(b.startDate);
    });

    available.forEach(w => {
        const opt = document.createElement('option');
        opt.value = String(w.id);
        const dateLabel = w.startDate ? ` — ${formatLocalDate(w.startDate)}` : '';
        opt.textContent = `${w.title || 'Untitled'}${dateLabel}`;
        sel.appendChild(opt);
    });

    // If no workshops, add a disabled placeholder
    if (available.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'No workshops available';
        opt.disabled = true;
        sel.appendChild(opt);
    }

    // Check if there is a 'register' param in URL to auto-select
    const params = new URLSearchParams(window.location.search);
    const regId = params.get('register');
    if (regId) {
        sel.value = regId;
        // Also scroll to it
        const section = document.querySelector('.workshop-registration');
        if (section) section.scrollIntoView({behavior: 'smooth'});
    }
}