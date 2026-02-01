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
    await loadActivities();
    await loadTeachers();
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

async function loadActivities() {
    try {
        const activities = await window.fetchJson(`${window.API_BASE_URL}/activities`);
        const select = document.getElementById('groupActivity');
        if (select) {
            const options = (activities || []).map(a => `<option value="${a.id}">${window.escapeHtml(a.titleEn || a.titleDe)}</option>`).join('');
            select.innerHTML += options;
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadTeachers() {
    try {
        const sel = document.getElementById('groupTeacher');
        if (sel) {
            const data = await window.fetchJson(`${window.API_BASE_URL}/users/role/TEACHER`);
            const list = Array.isArray(data) ? data : data?.content || [];
            const options = list.map(u => {
                const name = `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email || `User #${u.id}`;
                return `<option value="${u.id}">${window.escapeHtml(name)}</option>`;
            }).join('');
            sel.innerHTML += options;
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadGroups(workshopId = '') {
    const list = document.getElementById('groupsList');
    if (!list) return;
    try {
        const url = workshopId ? `${window.API_BASE_URL}/groups?workshopId=${workshopId}` : `${window.API_BASE_URL}/groups`;
        const groups = await window.fetchJson(url);
        list.innerHTML = (groups || []).map(g => {
            const title = g.titleEn || g.titleDe || g.titleUa || 'Unnamed Group';
            const wId = g.workshop ? g.workshop.id : (g.workshopId || '—');
            return `
                <div class="group-item" style="padding: 10px; border-bottom: 1px solid #eee;">
                    <strong>${window.escapeHtml(title)}</strong> (Workshop ID: ${wId})<br/>
                    <small>Capacity: ${g.enrolledCount ?? 0}/${g.capacity}</small>
                </div>
            `;
        }).join('') || '<p>No groups found.</p>';
    } catch (err) {
        console.error(err);
        list.innerHTML = '<p>Error loading groups.</p>';
    }
}

async function createGroup() {
    const workshopId = parseInt(document.getElementById('groupWorkshop').value);
    const activityId = document.getElementById('groupActivity').value;
    const teacherId = document.getElementById('groupTeacher').value;
    const workshop = allWorkshops.find(w => w.id === workshopId);
    
    const payload = {
        workshop: { id: workshopId },
        titleDe: document.getElementById('groupTitle').value,
        titleEn: document.getElementById('groupTitle').value,
        titleUa: document.getElementById('groupTitle').value,
        capacity: parseInt(document.getElementById('groupCap').value),
        capacityLeft: parseInt(document.getElementById('groupCap').value),
        startDateTime: document.getElementById('groupStartLocal').value,
        endDateTime: document.getElementById('groupEndLocal').value,
        active: true
    };

    if (activityId) payload.activity = { id: parseInt(activityId) };
    if (teacherId) payload.teacher = { id: parseInt(teacherId) };

    // If workshop has these, we can pass them along (if the API supports it)
    if (workshop) {
        if (workshop.ageGroup) payload.ageGroup = { id: workshop.ageGroup.id };
        if (workshop.language) payload.language = { id: workshop.language.id };
        // Workshop might not have activity directly
    }

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
