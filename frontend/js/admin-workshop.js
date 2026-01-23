// frontend/js/admin-workshop.js

document.addEventListener('DOMContentLoaded', init);

let cachedWorkshops = [];

async function init() {
    if (!window.API_BASE_URL) {
        console.error('API base not defined');
        return;
    }

    bindEditor();
    await loadAndRenderWorkshops();
}

/* =========================
   Main bindings
========================= */

function bindEditor() {
    const form = document.getElementById('workshopForm');
    const clearBtn = document.getElementById('clearWorkshopBtn');

    clearBtn?.addEventListener('click', clearForm);

    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveWorkshop();
    });
}

/* =========================
   Edit handler (GLOBAL)
========================= */

window.editWorkshop = async function (id) {
    if (!id) return;

    try {
        const res = await fetch(`${API_BASE_URL}/workshops/${id}`, {
            headers: getAuthHeaders()
        });

        if (!res.ok) {
            console.error('Failed to load workshop', id, res.status);
            return;
        }

        const w = await res.json();

        document.getElementById('workshopId').value = w.id;
        document.getElementById('wTitle').value = w.title || '';
        document.getElementById('wDescription').value = w.shortDescription || '';
        document.getElementById('wStart').value = toDateInput(w.startDate);
        document.getElementById('wEnd').value = toDateInput(w.endDate);
        document.getElementById('wPrice').value = w.price ?? '';
        document.getElementById('wMax').value = w.maxParticipants ?? '';
        document.getElementById('wTeacher').value = w.teacherId || '';
        document.getElementById('wVenue').value = w.venueId || '';
        document.getElementById('wLanguage').value = w.language || '';

        document.getElementById('formTitle').textContent = 'Edit workshop';
        document.getElementById('workshopFormResult').innerHTML = '';

        document.getElementById('manageGrid')
            .scrollIntoView({ behavior: 'smooth', block: 'start' });

    } catch (err) {
        console.error('editWorkshop error', err);
    }
};

/* =========================
   Save
========================= */

async function saveWorkshop() {
    const id = document.getElementById('workshopId').value;

    const payload = {
        title: document.getElementById('wTitle').value.trim(),
        shortDescription: document.getElementById('wDescription').value.trim(),
        startDate: document.getElementById('wStart').value || null,
        endDate: document.getElementById('wEnd').value || null,
        price: document.getElementById('wPrice').value || null,
        maxParticipants: document.getElementById('wMax').value || null,
        teacherId: document.getElementById('wTeacher').value || null,
        venueId: document.getElementById('wVenue').value || null,
        language: document.getElementById('wLanguage').value || null
    };

    const method = id ? 'PUT' : 'POST';
    const url = id
        ? `${API_BASE_URL}/admin/workshops/${id}`
        : `${API_BASE_URL}/admin/workshops`;

    const res = await fetch(url, {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...getAuthHeaders()
        },
        body: JSON.stringify(payload)
    });

    const resultBox = document.getElementById('workshopFormResult');

    if (!res.ok) {
        resultBox.innerHTML = `<span class="error">Save failed</span>`;
        return;
    }

    resultBox.innerHTML = `<span class="success">Saved</span>`;
    clearForm();
    await loadAndRenderWorkshops();
}

/* =========================
   Load & render list
========================= */

async function loadAndRenderWorkshops() {
    const list = document.getElementById('workshopsList');
    list.innerHTML = '<div class="admin-empty">Loading workshops…</div>';

    try {
        const res = await fetch(`${API_BASE_URL}/workshops`, {
            headers: getAuthHeaders()
        });

        const raw = await safeJsonOrEmpty(res);
        cachedWorkshops = Array.isArray(raw) ? raw : raw?.content || [];

        if (cachedWorkshops.length === 0) {
            list.innerHTML = '<div class="admin-empty">No workshops</div>';
            return;
        }

        list.innerHTML = cachedWorkshops.map(renderWorkshopRow).join('');

    } catch (err) {
        console.error(err);
        list.innerHTML = '<div class="admin-empty">Error loading</div>';
    }
}

function renderWorkshopRow(w) {
    const start = w.startDate ? formatDate(w.startDate) : 'TBA';

    return `
    <div class="admin-workshop-item">
        <div class="admin-workshop-main">
            <strong>${escapeHtml(w.title)}</strong>
            <span class="badge ${w.status?.toLowerCase() || 'draft'}">${w.status || 'DRAFT'}</span>
        </div>
        <div class="admin-workshop-meta">Start: ${start}</div>
        <div class="admin-workshop-actions">
            <button class="btn btn-outline btn-sm" onclick="editWorkshop(${w.id})">Edit</button>
        </div>
    </div>`;
}

/* =========================
   Utils
========================= */

function clearForm() {
    document.getElementById('workshopForm').reset();
    document.getElementById('workshopId').value = '';
    document.getElementById('formTitle').textContent = 'Create / Edit workshop';
}

function toDateInput(d) {
    if (!d) return '';
    return new Date(d).toISOString().slice(0, 10);
}

function formatDate(d) {
    return new Date(d).toLocaleDateString('de-DE');
}

function escapeHtml(s) {
    const div = document.createElement('div');
    div.textContent = s || '';
    return div.innerHTML;
}

async function safeJsonOrEmpty(res) {
    try {
        return await res.json();
    } catch {
        return [];
    }
}