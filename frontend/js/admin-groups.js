// frontend/js/admin-groups.js
document.addEventListener('DOMContentLoaded', init);

let allWorkshops = [];

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') return;
    if (!window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }
    await loadWorkshops();
    await loadGroups();
    bindEvents();
}

function bindEvents() {
    document.getElementById('groupForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createGroup();
    });
    
    document.getElementById('groupsWorkshopFilter')?.addEventListener('change', async (e) => {
        await loadGroups(e.target.value);
    });
}

async function loadWorkshops() {
    try {
        allWorkshops = await window.fetchJson(`${window.API_BASE_URL}/workshops`);
        const filter = document.getElementById('groupsWorkshopFilter');
        const select = document.getElementById('groupWorkshop');
        
        const options = (allWorkshops || []).map(w => `<option value="${w.id}">${window.escapeHtml(w.title)}</option>`).join('');
        if (filter) filter.innerHTML += options;
        if (select) select.innerHTML += options;
    } catch (err) {
        console.error(err);
    }
}

async function loadGroups(workshopId = '') {
    const list = document.getElementById('groupsList');
    if (!list) return;
    try {
        // Assume groups can be fetched via workshop or all groups
        // If no all-groups endpoint, we might need to iterate or fix backend
        // For now, assume a generic groups endpoint or filter by workshop
        const url = workshopId ? `${window.API_BASE_URL}/groups?workshopId=${workshopId}` : `${window.API_BASE_URL}/groups`;
        const groups = await window.fetchJson(url);
        list.innerHTML = (groups || []).map(g => `
            <div class="group-item" style="padding: 10px; border-bottom: 1px solid #eee;">
                <strong>${window.escapeHtml(g.name || g.titleEn)}</strong> (Workshop ID: ${g.workshopId})<br/>
                <small>Capacity: ${g.enrolledCount}/${g.capacity}</small>
            </div>
        `).join('') || '<p>No groups found.</p>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<p>Error loading groups.</p>';
    }
}

async function createGroup() {
    const payload = {
        workshopId: parseInt(document.getElementById('groupWorkshop').value),
        name: document.getElementById('groupTitle').value,
        capacity: parseInt(document.getElementById('groupCap').value),
        startDateTime: document.getElementById('groupStartLocal').value,
        endDateTime: document.getElementById('groupEndLocal').value
    };
    try {
        await window.fetchJson(`${window.API_BASE_URL}/groups`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        alert('Group created');
        document.getElementById('groupForm').reset();
        await loadGroups(document.getElementById('groupsWorkshopFilter').value);
    } catch (err) {
        alert('Error: ' + err.message);
    }
}
