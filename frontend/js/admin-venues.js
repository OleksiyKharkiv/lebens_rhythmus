// frontend/js/admin-venues.js
document.addEventListener('DOMContentLoaded', init);

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') return;
    if (!window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }
    await loadVenues();
    bindEvents();
}

function bindEvents() {
    const form = document.getElementById('venueForm');
    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createVenue();
    });
}

async function loadVenues() {
    const list = document.getElementById('venuesList');
    if (!list) return;
    try {
        const venues = await window.fetchJson(`${window.API_BASE_URL}/venues`);
        list.innerHTML = (venues || []).map(v => `
            <div class="venue-item" style="padding: 10px; border-bottom: 1px solid #eee;">
                <strong>${window.escapeHtml(v.name)}</strong><br/>
                <small>${window.escapeHtml(v.address || '')}, ${window.escapeHtml(v.city || '')}</small>
            </div>
        `).join('') || '<p>No venues found.</p>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<p>Error loading venues.</p>';
    }
}

async function createVenue() {
    const payload = {
        name: document.getElementById('vName').value,
        address: document.getElementById('vAddress').value,
        city: document.getElementById('vCity').value,
        capacity: parseInt(document.getElementById('vCapacity').value) || null
    };
    try {
        await window.fetchJson(`${window.API_BASE_URL}/venues`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        alert('Venue created');
        document.getElementById('venueForm').reset();
        await loadVenues();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}
