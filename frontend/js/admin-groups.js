// frontend/js/admin-groups.js
document.addEventListener('DOMContentLoaded', init);

let allWorkshops = [];
let editingGroupId = null;

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
        if (editingGroupId) {
            await updateGroup(editingGroupId);
        } else {
            await createGroup();
        }
    });

    document.getElementById('groupsWorkshopFilter')?.addEventListener('change', async (e) => {
        await loadGroups(e.target.value);
    });

    // reset editing on form reset (if any)
    document.getElementById('groupForm')?.addEventListener('reset', () => {
        editingGroupId = null;
        document.querySelector('#groupForm button[type="submit"]').textContent = 'Create';
    });
}

async function loadWorkshops() {
    try {
        const raw = await window.fetchJson(`${window.API_BASE_URL}/workshops`);
        allWorkshops = Array.isArray(raw) ? raw : (raw?.content || []);

        const filter = document.getElementById('groupsWorkshopFilter');
        const select = document.getElementById('groupWorkshop');

        const options = (allWorkshops || [])
            .map(w => `<option value="${w.id}">${window.escapeHtml(w.title || w.workshopName || w.titleEn || 'Workshop #' + w.id)}</option>`)
            .join('');

        if (filter) filter.innerHTML = `<option value="">All workshops</option>` + options;
        if (select) select.innerHTML = `<option value="">— choose —</option>` + options;
    } catch (err) {
        console.error('loadWorkshops failed', err);
    }
}

async function loadActivities() {
    try {
        const activities = await window.fetchJson(`${window.API_BASE_URL}/activities`);
        const select = document.getElementById('groupActivity');
        if (select) {
            const options = (activities || []).map(a => `<option value="${a.id}">${window.escapeHtml(a.titleEn || a.titleDe || 'Activity #' + a.id)}</option>`).join('');
            select.innerHTML = `<option value="">— choose (optional) —</option>` + options;
        }
    } catch (err) {
        console.error('loadActivities failed', err);
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
            sel.innerHTML = `<option value="">— none —</option>` + options;
        }
    } catch (err) {
        console.error('loadTeachers failed', err);
    }
}

async function loadGroups(workshopId = '') {
    const list = document.getElementById('groupsList');
    if (!list) return;
    try {
        const url = workshopId ? `${window.API_BASE_URL}/groups?workshopId=${workshopId}` : `${window.API_BASE_URL}/groups`;
        const groups = await window.fetchJson(url);

        if (!groups || groups.length === 0) {
            list.innerHTML = '<div class="admin-empty">No groups found.</div>';
            return;
        }

        list.innerHTML = (groups || []).map(g => {
            const title = g.titleEn || g.titleDe || g.titleUa || g.name || 'Unnamed Group';
            const workshopLabel = g.workshopTitle
                ? window.escapeHtml(g.workshopTitle)
                : (g.workshopId ?? '—');

            const cap = g.capacity ?? '—';
            const enrolled = g.enrolledCount ?? 0;
            return `
                <div class="group-item" style="padding: 10px; border-bottom: 1px solid #eee; display:flex;justify-content:space-between;align-items:center;">
                    <div>
                        <strong>${window.escapeHtml(title)}</strong>
                        <div style="font-size:.9rem;color:var(--muted);">
                            Workshop: ${workshopLabel}
                        </div>
                        
                        <small>Capacity: ${enrolled}/${cap}</small>
                    </div>
                    <div style="display:flex;gap:.5rem;align-items:center;">
                        <button class="btn btn-outline btn-sm" onclick="editGroup(${g.id})">Edit</button>
                        <button class="btn btn-ghost btn-sm" onclick="deleteGroup(${g.id})">Delete</button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error('loadGroups failed', err);
        list.innerHTML = '<p>Error loading groups.</p>';
    }
}

async function createGroup() {
    const workshopId = parseInt(document.getElementById('groupWorkshop').value);
    if (!workshopId) return alert('Choose workshop');
    const activityId = document.getElementById('groupActivity').value;
    const teacherId = document.getElementById('groupTeacher').value;

    const title = document.getElementById('groupTitle').value.trim();
    const capacity = parseInt(document.getElementById('groupCap').value) || 0;
    const payload = {
        workshop: {id: workshopId},
        titleDe: title,
        titleEn: title,
        titleUa: title,
        capacity: capacity,
        capacityLeft: capacity,
        startDateTime: document.getElementById('groupStartLocal').value || null,
        endDateTime: document.getElementById('groupEndLocal').value || null,
        active: true
    };

    if (activityId) payload.activity = {id: parseInt(activityId)};
    if (teacherId) payload.teacher = {id: parseInt(teacherId)};

    // If we have workshop in memory, pass ageGroup/language if present
    const workshop = allWorkshops.find(w => Number(w.id) === Number(workshopId));
    if (workshop) {
        if (workshop.ageGroup) payload.ageGroup = {id: workshop.ageGroup.id};
        if (workshop.language) payload.language = {id: workshop.language.id};
    }

    try {
        await window.fetchJson(`${window.API_BASE_URL}/groups`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', ...window.getAuthHeaders?.()},
            body: JSON.stringify(payload)
        });
        alert('Group created');
        document.getElementById('groupForm').reset();
        await loadGroups(document.getElementById('groupsWorkshopFilter').value);
    } catch (err) {
        alert('Error: ' + (err.message || err));
    }
}

async function updateGroup(id) {
    const workshopId = parseInt(document.getElementById('groupWorkshop').value);
    if (!workshopId) return alert('Choose workshop');
    const activityId = document.getElementById('groupActivity').value;
    const teacherId = document.getElementById('groupTeacher').value;

    const title = document.getElementById('groupTitle').value.trim();
    const capacity = parseInt(document.getElementById('groupCap').value) || 0;
    const payload = {
        workshop: {id: workshopId},
        titleDe: title,
        titleEn: title,
        titleUa: title,
        capacity: capacity,
        capacityLeft: capacity,
        startDateTime: document.getElementById('groupStartLocal').value || null,
        endDateTime: document.getElementById('groupEndLocal').value || null,
        active: true
    };

    if (activityId) payload.activity = {id: parseInt(activityId)};
    if (teacherId) payload.teacher = {id: parseInt(teacherId)};

    try {
        await window.fetchJson(`${window.API_BASE_URL}/groups/${id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json', ...window.getAuthHeaders?.()},
            body: JSON.stringify(payload)
        });
        alert('Group updated');
        document.getElementById('groupForm').reset();
        editingGroupId = null;
        document.querySelector('#groupForm button[type="submit"]').textContent = 'Create';
        await loadGroups(document.getElementById('groupsWorkshopFilter').value);
    } catch (err) {
        alert('Update error: ' + (err.message || err));
    }
}

window.editGroup = async function (id) {
    try {
        const g = await window.fetchJson(`${window.API_BASE_URL}/groups/${id}`);
        if (!g) return alert('Group not found');

        editingGroupId = id;
        // fill form
        if (g.workshopId) document.getElementById('groupWorkshop').value = String(g.workshopId);
        if (g.activityId) document.getElementById('groupActivity').value = String(g.activityId);
        if (g.teacherId) document.getElementById('groupTeacher').value = String(g.teacherId);

        const title = g.titleEn || g.titleDe || g.titleUa || g.name || '';
        document.getElementById('groupTitle').value = title;
        document.getElementById('groupCap').value = g.capacity ?? '';
        document.getElementById('groupStartLocal').value = g.startDateTime ? new Date(g.startDateTime).toISOString().slice(0, 16) : '';
        document.getElementById('groupEndLocal').value = g.endDateTime ? new Date(g.endDateTime).toISOString().slice(0, 16) : '';

        document.querySelector('#groupForm button[type="submit"]').textContent = 'Update';
        // scroll to form
        document.querySelector('aside.dash-card, aside')?.scrollIntoView({behavior: 'smooth', block: 'center'});
    } catch (err) {
        console.error('editGroup failed', err);
        alert('Failed to load group: ' + (err.message || err));
    }
};

window.deleteGroup = async function (id) {
    if (!confirm('Delete group?')) return;
    try {
        await window.fetchJson(`${window.API_BASE_URL}/groups/${id}`, {method: 'DELETE'});
        await loadGroups(document.getElementById('groupsWorkshopFilter').value);
    } catch (err) {
        alert('Delete failed: ' + (err.message || err));
    }
};