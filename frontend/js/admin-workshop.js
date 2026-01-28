// frontend/js/admin-workshop.js
// Admin: load list (public), edit detail (detail DTO), create/update (admin protected endpoints).
// Assumes window.API_BASE_URL present. Uses window.getAuthHeaders() if available.

document.addEventListener('DOMContentLoaded', init);

let cachedWorkshops = [];

function apiBase() {
    return window.API_BASE_URL || '/api/v1';
}

function authHeaders() {
    if (typeof window.getAuthHeaders === 'function') return window.getAuthHeaders();
    // fallback: try common tokens in localStorage
    const token = localStorage.getItem('token') || localStorage.getItem('accessToken') || localStorage.getItem('jwt');
    if (token) return { Authorization: `Bearer ${token}` };
    return {};
}

async function init() {
    if (!apiBase()) {
        console.error('API base not defined');
        return;
    }
    bindEditor();
    await loadAndRenderWorkshops();
    // try populate selects (best-effort)
    try { await loadTeacherAndVenueSelects(); } catch { /* ignore */ }
}

/* =========================
   BINDINGS
========================= */
function bindEditor() {
    const form = document.getElementById('workshopForm');
    const clearBtn = document.getElementById('clearWorkshopBtn');
    const openGroupsBtn = document.getElementById('openGroupsBtn');

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
        // open the groups panel and load groups for workshop (if you have an endpoint)
        document.getElementById('groupsWorkshopId').value = id;
        document.getElementById('groupsWorkshopTitle').textContent = document.getElementById('wTitle').value || '—';
        document.getElementById('workshopGroupsPanel').style.display = 'block';
        // optional: loadGroupsForWorkshop(id);
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
        const res = await fetch(`${apiBase()}/workshops`);
        const raw = await safeJsonOrEmpty(res);
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
    const start = w.startDate ? formatDate(w.startDate) : 'TBA';
    const statusClass = (w.status || 'DRAFT').toLowerCase();
    const title = escapeHtml(w.title || w.workshopName || 'Untitled');
    const venue = escapeHtml(w.venueName || '');

    return `
    <div class="admin-workshop-item" data-id="${w.id}" style="padding:.65rem;border-bottom:1px dashed rgba(0,0,0,0.04);">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:1rem;">
            <div>
                <strong>${title}</strong>
                <div class="help" style="margin-top:.25rem;font-size:.9rem;color:var(--muted);">Start: ${start} ${venue ? ' • ' + venue : ''}</div>
            </div>
            <div style="display:flex;gap:.5rem;align-items:center;">
                <button class="btn btn-outline btn-sm" onclick="editWorkshop(${w.id})">Edit</button>
                <button class="btn btn-ghost btn-sm" onclick="openGroupsFor(${w.id}, '${escapeHtmlForAttr(w.title || '')}')">Groups</button>
            </div>
        </div>
    </div>`;
}

function openGroupsFor(id, title) {
    document.getElementById('groupsWorkshopId').value = id;
    document.getElementById('groupsWorkshopTitle').textContent = title || '—';
    document.getElementById('workshopGroupsPanel').style.display = 'block';
    // optionally: load groups via API
}

/* =========================
   EDIT (fetch detail) and FILL FORM
========================= */
window.editWorkshop = async function (id) {
    if (!id) return;
    try {
        // detail endpoint (public) returns WorkshopDetailDTO
        const res = await fetch(`${apiBase()}/workshops/${id}`, { headers: authHeaders() });
        if (!res.ok) {
            console.error('Failed to load workshop', id, res.status);
            return;
        }
        const w = await res.json();

        // fill fields — be defensive: some DTOs may not include all admin fields (maxParticipants etc.)
        document.getElementById('workshopId').value = w.id ?? '';
        document.getElementById('wTitle').value = w.title ?? w.workshopName ?? '';
        document.getElementById('wDescription').value = w.description ?? w.shortDescription ?? '';
        document.getElementById('wStart').value = toDateInput(w.startDate);
        document.getElementById('wEnd').value = toDateInput(w.endDate);
        document.getElementById('wPrice').value = (w.price != null) ? String(w.price) : '';
        // maxParticipants might be missing in detail DTO — try to read if present, else blank
        document.getElementById('wMax').value = (w.maxParticipants != null) ? w.maxParticipants : (w.maxParticipants ?? w.max_participants ?? '');
        // teacher from a nested teacher object if present
        try {
            document.getElementById('wTeacher').value = w.teacher?.id ?? '';
        } catch { document.getElementById('wTeacher').value = ''; }
        // venue: detail DTO gives venueName but not id — we attempt to set by venueId if present, else leave
        document.getElementById('wVenue').value = w.venueId ?? '';
        document.getElementById('wLanguage').value = w.language ?? '';

        document.getElementById('formTitle').textContent = 'Edit workshop';
        document.getElementById('workshopFormResult').innerHTML = '';
        // scroll to form
        document.getElementById('manageGrid')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
        console.error('editWorkshop error', err);
    }
};

/* =========================
   SAVE (create/update)
   build payload matching WorkshopCreateDTO on backend
========================= */
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
        status: 'DRAFT' // default; toggle Publish button will call a separate flow if needed
    };

    const url = isEdit ? `${apiBase()}/workshops/${id}` : `${apiBase()}/workshops`;
    const method = isEdit ? 'PUT' : 'POST';

    const resultBox = document.getElementById('workshopFormResult');
    resultBox.innerHTML = ''; // clear

    try {
        const res = await fetch(url, {
            method,
            headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const txt = await res.text().catch(()=>null);
            console.error('Save failed', res.status, txt);
            resultBox.innerHTML = `<div class="notify err">Save failed (${res.status})</div>`;
            return;
        }

        const saved = await safeJsonOrEmpty(res);
        resultBox.innerHTML = `<div class="notify ok">Saved</div>`;
        clearForm();
        await loadAndRenderWorkshops();
    } catch (err) {
        console.error('saveWorkshop error', err);
        resultBox.innerHTML = `<div class="notify err">Network error</div>`;
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
        if (sel) {
            // try endpoint variants
            const r1 = await fetch(`${apiBase()}/users?role=TEACHER`, { headers: authHeaders() });
            const data = r1.ok ? await safeJsonOrEmpty(r1) : [];
            const list = Array.isArray(data) ? data : data?.content || [];
            // clear existing except default option
            const defaultOpt = sel.querySelector('option') ? sel.querySelector('option').outerHTML : '<option value="">— none —</option>';
            sel.innerHTML = defaultOpt;
            list.forEach(u => {
                const opt = document.createElement('option');
                opt.value = u.id;
                opt.textContent = `${u.firstName || ''} ${u.lastName || ''}`.trim() || (u.email || `user#${u.id}`);
                sel.appendChild(opt);
            });
        }
    } catch (e) { /* ignore */ }

    // venues
    try {
        const selV = document.getElementById('wVenue');
        if (selV) {
            const r2 = await fetch(`${apiBase()}/venues`, { headers: authHeaders() });
            const data = r2.ok ? await safeJsonOrEmpty(r2) : [];
            const list = Array.isArray(data) ? data : data?.content || [];
            const defaultOpt = selV.querySelector('option') ? selV.querySelector('option').outerHTML : '<option value="">— none —</option>';
            selV.innerHTML = defaultOpt;
            list.forEach(v => {
                const opt = document.createElement('option');
                opt.value = v.id;
                opt.textContent = v.name || `venue#${v.id}`;
                selV.appendChild(opt);
            });
        }
    } catch (e) { /* ignore */ }
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
    } catch { return d; }
}

function formatDate(d) {
    try { return new Date(d).toLocaleDateString('de-DE'); } catch { return d; }
}

function escapeHtml(s) {
    const div = document.createElement('div');
    div.textContent = s || '';
    return div.innerHTML;
}

function escapeHtmlForAttr(s) {
    return (s || '').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

async function safeJsonOrEmpty(res) {
    const ct = res?.headers?.get ? (res.headers.get('content-type') || '') : '';
    if (!res) return [];
    if (!ct.includes('application/json')) {
        const t = await res.text().catch(()=>'');
        if (!t) return [];
        try { return JSON.parse(t); } catch { return []; }
    }
    try { return await res.json(); } catch { return []; }
}
