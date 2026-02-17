// frontend/js/admin-workshop.js
// Admin: load list (public), edit detail (detail DTO), create/update (admin protected endpoints).
// Assumes window.API_BASE_URL present. Uses window.getAuthHeaders() if available.

document.addEventListener('DOMContentLoaded', init);

let cachedWorkshops = [];

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') {
        console.error('API base not defined');
        return;
    }
    bindEditor();
    await loadAndRenderWorkshops();
    // try populate selects (best-effort)
    try {
        await loadTeacherAndVenueSelects();
        await loadActivitySelect();
    } catch (e) {
        // non-fatal
        console.warn('selects load failed', e);
    }
}

/* =========================
   BINDINGS
========================= */
function bindEditor() {
    const form = document.getElementById('workshopForm');
    const clearBtn = document.getElementById('clearWorkshopBtn');
    const openGroupsBtn = document.getElementById('openGroupsBtn');
    const groupForm = document.getElementById('groupCreateForm');
    const closeGroupsBtn = document.getElementById('closeGroupsPanel');
    const refreshGroupsBtn = document.getElementById('refreshGroupsBtn');

    clearBtn?.addEventListener('click', clearForm);
    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveWorkshop();
    });

    openGroupsBtn?.addEventListener('click', () => {
        const id = document.getElementById('workshopId').value;
        if (!id) {
            alert('Open groups: create or select a workshop first.');
            return;
        }
        openGroupsFor(id, document.getElementById('wTitle').value || '—');
    });

    groupForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await createGroupInPanel();
    });

    closeGroupsBtn?.addEventListener('click', () => {
        document.getElementById('workshopGroupsPanel').style.display = 'none';
    });

    refreshGroupsBtn?.addEventListener('click', () => {
        const id = document.getElementById('groupsWorkshopId').value;
        if (id) loadGroupsForWorkshop(id);
    });
}

/* =========================
   LIST LOAD / RENDER (public GET)
========================= */
async function loadAndRenderWorkshops() {
    const list = document.getElementById('workshopsList');
    if (!list) return;
    list.innerHTML = '<div class="admin-empty">Loading workshops…</div>';

    try {
        const raw = await window.fetchJson(`${window.API_BASE_URL}/workshops`);
        cachedWorkshops = Array.isArray(raw) ? raw : (raw?.content || []);
        if (!cachedWorkshops || cachedWorkshops.length === 0) {
            list.innerHTML = '<div class="admin-empty">No workshops</div>';
            return;
        }
        list.innerHTML = cachedWorkshops.map(renderWorkshopRow).join('');
    } catch (err) {
        console.error('loadAndRenderWorkshops error', err);
        list.innerHTML = '<div class="admin-empty">Error loading</div>';
    }
}

function renderWorkshopRow(w) {
    const start = w.startDate ? window.formatLocalDate(w.startDate) : 'TBA';
    const title = window.escapeHtml(w.title || w.workshopName || 'Untitled');
    const venue = window.escapeHtml(w.venueName || '');
    return `
    <div class="admin-workshop-item" data-id="${w.id}" style="padding:.65rem;border-bottom:1px dashed rgba(0,0,0,0.04);">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:1rem;">
            <div>
                <strong>${title}</strong>
                <div class="help" style="margin-top:.25rem;font-size:.9rem;color:var(--muted);">Start: ${start} ${venue ? ' • ' + venue : ''}</div>
            </div>
            <div style="display:flex;gap:.5rem;align-items:center;">
                <button class="btn btn-outline btn-sm" onclick="editWorkshop(${w.id})">Edit</button>
                <button class="btn btn-ghost btn-sm" onclick="openGroupsFor(${w.id}, '${escapeHtmlForAttr(w.title || w.workshopName || '')}')">Groups</button>
            </div>
        </div>
    </div>`;
}

function openGroupsFor(id, title) {
    document.getElementById('groupsWorkshopId').value = id;
    document.getElementById('groupsWorkshopTitle').textContent = title || '—';
    document.getElementById('workshopGroupsPanel').style.display = 'block';
    loadGroupsForWorkshop(id);
}

async function loadGroupsForWorkshop(workshopId) {
    const container = document.getElementById('groupsListContainer');
    if (!container) return;
    container.innerHTML = '<div class="admin-empty">Loading groups…</div>';
    try {
        const groups = await window.fetchJson(`${window.API_BASE_URL}/groups?workshopId=${workshopId}`);
        if (!groups || groups.length === 0) {
            container.innerHTML = '<div class="admin-empty">No groups yet — add the first group.</div>';
            return;
        }
        container.innerHTML = groups.map(g => `
            <div style="padding:.5rem; border-bottom:1px solid #eee; display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <strong>${window.escapeHtml(g.titleEn || g.titleDe || 'Unnamed')}</strong>
                    <div style="font-size:.85rem; color:#666;">
                        ${window.formatLocalDate(g.startDateTime)} • Capacity: ${g.enrolledCount ?? 0}/${g.capacity}
                    </div>
                </div>
                <button class="btn btn-ghost btn-sm" onclick="deleteGroup(${g.id})">Delete</button>
            </div>
        `).join('');
    } catch (err) {
        console.error('loadGroupsForWorkshop error', err);
        container.innerHTML = '<div class="admin-empty">Error loading groups.</div>';
    }
}

async function createGroupInPanel() {
    const workshopId = document.getElementById('groupsWorkshopId').value;
    const title = document.getElementById('gTitle').value.trim();
    const capacity = parseInt(document.getElementById('gCapacity').value);
    const activityId = document.getElementById('gActivity').value;
    const teacherId = document.getElementById('gTeacher').value;

    // Find workshop in cached list to get language/ageGroup
    const workshop = cachedWorkshops.find(w => String(w.id) === String(workshopId));

    const payload = {
        workshop: {id: Number(workshopId)},
        titleDe: title,
        titleEn: title,
        titleUa: title,
        capacity: capacity,
        capacityLeft: capacity,
        startDateTime: document.getElementById('gStart').value || null,
        endDateTime: document.getElementById('gEnd').value || null,
        active: true
    };

    if (activityId) payload.activity = {id: Number(activityId)};
    if (teacherId) payload.teacher = {id: Number(teacherId)};

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
        document.getElementById('groupCreateForm').reset();
        loadGroupsForWorkshop(workshopId);
    } catch (err) {
        alert('Failed to create group: ' + (err.message || err));
    }
}

window.deleteGroup = async function (id) {
    if (!confirm('Delete group?')) return;
    try {
        await window.fetchJson(`${window.API_BASE_URL}/groups/${id}`, {method: 'DELETE'});
        loadGroupsForWorkshop(document.getElementById('groupsWorkshopId').value);
    } catch (err) {
        alert('Delete failed: ' + (err.message || err));
    }
}

/* =========================
   EDIT (fetch detail) and FILL FORM
========================= */
window.editWorkshop = async function (id) {
    if (!id) return;
    try {
        const w = await window.fetchJson(`${window.API_BASE_URL}/workshops/${id}`);

        // fill fields — be defensive: some DTOs may not include all admin fields (maxParticipants etc.)
        document.getElementById('workshopId').value = w.id ?? '';
        document.getElementById('wTitle').value = w.title ?? w.workshopName ?? '';
        document.getElementById('wDescription').value = w.description ?? w.shortDescription ?? '';
        document.getElementById('wStart').value = toDateInput(w.startDate);
        document.getElementById('wEnd').value = toDateInput(w.endDate);
        document.getElementById('wPrice').value = (w.price != null) ? String(w.price) : '';
        document.getElementById('wMax').value = (w.maxParticipants != null) ? w.maxParticipants : '';

        // teacher (detail DTO includes teacher object)
        try {
            const teacherSel = document.getElementById('wTeacher');
            if (teacherSel) {
                // teacher may be object {id,...} or id
                const tid = w.teacher?.id ?? w.teacherId ?? null;
                teacherSel.value = tid ?? '';
            }
        } catch (e) { /* ignore */
        }

        // venue: detail DTO may only include venueName (no id). Try id first, else match by name.
        const venueSel = document.getElementById('wVenue');
        try {
            const vid = w.venueId ?? w.venue?.id ?? null;
            if (venueSel) {
                if (vid) {
                    venueSel.value = String(vid);
                } else if (w.venueName) {
                    // try to find option matching venueName (case-insensitive, trim)
                    const name = String(w.venueName).trim().toLowerCase();
                    let found = false;
                    for (const opt of Array.from(venueSel.options)) {
                        if (String(opt.text).trim().toLowerCase() === name) {
                            venueSel.value = opt.value;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // not found — leave as empty (admin may re-select)
                        venueSel.value = '';
                    }
                } else {
                    venueSel.value = '';
                }
            }
        } catch (e) { /* ignore */
        }

        // language could be object or primitive
        try {
            document.getElementById('wLanguage').value = w.language?.id ?? w.language ?? '';
        } catch (e) { /* ignore */
        }

        document.getElementById('formTitle').textContent = 'Edit workshop';
        document.getElementById('workshopFormResult').innerHTML = '';
        // scroll to form
        document.getElementById('manageGrid')?.scrollIntoView({behavior: 'smooth', block: 'start'});
    } catch (err) {
        console.error('editWorkshop error', err);
    }
};

async function saveWorkshop() {
    const id = document.getElementById('workshopId').value;
    const isEdit = Boolean(id);

    const payload = {
        title: document.getElementById('wTitle').value.trim() || null,
        description: document.getElementById('wDescription').value.trim() || null,
        teacherId: (document.getElementById('wTeacher').value) ? Number(document.getElementById('wTeacher').value) : null,
        startDate: document.getElementById('wStart').value || null,
        endDate: document.getElementById('wEnd').value || null,
        venueId: (document.getElementById('wVenue').value) ? Number(document.getElementById('wVenue').value) : null,
        maxParticipants: document.getElementById('wMax').value ? Number(document.getElementById('wMax').value) : null,
        price: (document.getElementById('wPrice').value) ? Number(document.getElementById('wPrice').value) : null,
        status: 'DRAFT'
    };

    const url = isEdit ? `${window.API_BASE_URL}/workshops/${id}` : `${window.API_BASE_URL}/workshops`;
    const method = isEdit ? 'PUT' : 'POST';

    const resultBox = document.getElementById('workshopFormResult');
    resultBox.innerHTML = ''; // clear

    try {
        await window.fetchJson(url, {
            method,
            headers: {'Content-Type': 'application/json', ...window.getAuthHeaders?.()},
            body: JSON.stringify(payload)
        });

        resultBox.innerHTML = `<div class="notify ok">Saved</div>`;
        clearForm();
        await loadAndRenderWorkshops();
    } catch (err) {
        console.error('saveWorkshop error', err);
        resultBox.innerHTML = `<div class="notify err">${err.message || 'Network error'}</div>`;
    }
}

/* =========================
   Small helpers / selects loader (teachers/venues)
   Best-effort: tries /users?role=TEACHER and /venues endpoints if exist.
========================= */
async function loadTeacherAndVenueSelects() {
    // teachers
    try {
        const sel = document.getElementById('wTeacher');
        const gSel = document.getElementById('gTeacher'); // group teacher too
        if (sel || gSel) {
            const data = await window.fetchJson(`${window.API_BASE_URL}/users/role/TEACHER`);
            const list = Array.isArray(data) ? data : data?.content || [];

            const options = list.map(u => {
                const name = `${u.firstName || ''} ${u.lastName || ''}`.trim() || (u.email || `user#${u.id}`);
                return `<option value="${u.id}">${window.escapeHtml(name)}</option>`;
            }).join('');

            if (sel) sel.innerHTML = `<option value="">— none —</option>` + options;
            if (gSel) gSel.innerHTML = `<option value="">— none —</option>` + options;
        }
    } catch (e) {
        console.warn('load teachers failed', e); /* ignore */
    }

    // venues
    try {
        const selV = document.getElementById('wVenue');
        if (selV) {
            const data = await window.fetchJson(`${window.API_BASE_URL}/venues`);
            const list = Array.isArray(data) ? data : data?.content || [];
            const options = list.map(v => `<option value="${v.id}">${window.escapeHtml(v.name || v.venueName || 'Venue')}</option>`).join('');
            selV.innerHTML = `<option value="">— none —</option>` + options;
        }
    } catch (e) {
        console.warn('load venues failed', e); /* ignore */
    }
}

async function loadActivitySelect() {
    try {
        const sel = document.getElementById('gActivity');
        if (sel) {
            const activities = await window.fetchJson(`${window.API_BASE_URL}/activities`);
            const list = Array.isArray(activities) ? activities : (activities?.content || []);
            const options = list.map(a => `<option value="${a.id}">${window.escapeHtml(a.titleEn || a.titleDe)}</option>`).join('');
            sel.innerHTML = `<option value="">— choose (optional) —</option>` + options;
        }
    } catch (e) {
        console.warn('load activities failed', e); /* ignore */
    }
}

/* =========================
   UTIL
========================= */
function clearForm() {
    const form = document.getElementById('workshopForm');
    form?.reset();
    document.getElementById('workshopId').value = '';
    document.getElementById('formTitle').textContent = 'Create / Edit workshop';
    document.getElementById('workshopFormResult').innerHTML = '';
}

function toDateInput(d) {
    if (!d) return '';
    try {
        return new Date(d).toISOString().slice(0, 10);
    } catch {
        return d;
    }
}

function escapeHtmlForAttr(s) {
    return (s || '').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}