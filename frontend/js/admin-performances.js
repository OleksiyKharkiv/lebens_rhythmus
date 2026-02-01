// frontend/js/admin-performances.js
document.addEventListener('DOMContentLoaded', init);

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') return;
    if (!window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }
    await loadWorkshopsAndVenues();
    await loadPerformances();
    bindEvents();
}

function bindEvents() {
    document.getElementById('performanceForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createPerformance();
    });
}

async function loadWorkshopsAndVenues() {
    try {
        const workshops = await window.fetchJson(`${window.API_BASE_URL}/workshops`);
        const venues = await window.fetchJson(`${window.API_BASE_URL}/venues`);
        
        const wSelect = document.getElementById('pWorkshop');
        const vSelect = document.getElementById('pVenue');
        
        if (wSelect) wSelect.innerHTML += (workshops || []).map(w => `<option value="${w.id}">${window.escapeHtml(w.title)}</option>`).join('');
        if (vSelect) vSelect.innerHTML += (venues || []).map(v => `<option value="${v.id}">${window.escapeHtml(v.name)}</option>`).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadPerformances() {
    const list = document.getElementById('performancesList');
    if (!list) return;
    try {
        const perfs = await window.fetchJson(`${window.API_BASE_URL}/performances`);
        list.innerHTML = (perfs || []).map(p => `
            <div class="performance-item" style="padding: 10px; border-bottom: 1px solid #eee;">
                <strong>${window.escapeHtml(p.title)}</strong><br/>
                <small>${window.formatLocalDate(p.date)} | Venue: ${window.escapeHtml(p.venueName || 'TBA')}</small>
            </div>
        `).join('') || '<p>No performances found.</p>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<p>Error loading performances.</p>';
    }
}

async function createPerformance() {
    const payload = {
        title: document.getElementById('pTitle').value,
        workshopId: parseInt(document.getElementById('pWorkshop').value) || null,
        date: document.getElementById('pDate').value,
        venueId: parseInt(document.getElementById('pVenue').value) || null
    };
    try {
        await window.fetchJson(`${window.API_BASE_URL}/performances`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        alert('Performance created');
        document.getElementById('performanceForm').reset();
        await loadPerformances();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}
