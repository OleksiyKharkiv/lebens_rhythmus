// frontend/js/admin-activities.js
document.addEventListener('DOMContentLoaded', init);

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') return;
    if (!window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }
    await loadActivities();
    bindEvents();
}

function bindEvents() {
    document.getElementById('activityForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createActivity();
    });
}

async function loadActivities() {
    const list = document.getElementById('activitiesList');
    if (!list) return;
    try {
        const activities = await window.fetchJson(`${window.API_BASE_URL}/activities`);
        list.innerHTML = (activities || []).map(a => `
            <div class="activity-item" style="padding: 10px; border-bottom: 1px solid #eee;">
                <strong>${window.escapeHtml(a.titleEn || a.titleDe)}</strong><br/>
                <small>${window.formatPrice(a.price)} | ${a.durationMinutes} min</small>
            </div>
        `).join('') || '<p>No activities found.</p>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<p>Error loading activities.</p>';
    }
}

async function createActivity() {
    const payload = {
        titleDe: document.getElementById('aTitleDe').value,
        titleEn: document.getElementById('aTitleEn').value,
        titleUa: document.getElementById('aTitleEn').value, // Fallback
        descriptionDe: document.getElementById('aDescEn').value, // Fallback
        descriptionEn: document.getElementById('aDescEn').value,
        descriptionUa: document.getElementById('aDescEn').value, // Fallback
        price: parseFloat(document.getElementById('aPrice').value),
        durationMinutes: parseInt(document.getElementById('aDuration').value),
        active: true
    };
    try {
        await window.fetchJson(`${window.API_BASE_URL}/activities`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        alert('Activity created');
        document.getElementById('activityForm').reset();
        await loadActivities();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}