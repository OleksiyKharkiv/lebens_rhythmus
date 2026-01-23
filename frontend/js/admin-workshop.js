// frontend/js/admin-workshop.js
// Admin helper: create workshop + add groups to created workshop.
// Uses window.API_BASE_URL and window.getAuthHeaders()

document.addEventListener('DOMContentLoaded', () => {
    // === Auth + role guard ===
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '/pages/login/login.html';
        return;
    }
    let user = {};
    try { user = JSON.parse(localStorage.getItem('userData') || '{}'); } catch {}
    const allowed = ['ADMIN','TEACHER','BUSINESS_OWNER'];
    if (!allowed.includes((user.role || '').toUpperCase())) {
        document.body.innerHTML = '<main style="padding:2rem"><h2>Unauthorized</h2><p>Du hast keine Rechte für diese Seite.</p></main>';
        return;
    }

    // === DOM refs ===
    const form = document.getElementById('createWorkshopForm');
    const result = document.getElementById('createResult');
    const resetBtn = document.getElementById('resetBtn');

    const groupsSection = document.getElementById('groupsSection');
    const createdWorkshopTitle = document.getElementById('createdWorkshopTitle');
    const createdWorkshopId = document.getElementById('createdWorkshopId');
    const groupsList = document.getElementById('groupsList');
    const groupForm = document.getElementById('createGroupForm');

    // If page wasn't loaded with expected DOM, bail quietly
    if (!form) return;

    // === Create workshop ===
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        result.innerHTML = '';

        const payload = {
            title: document.getElementById('title').value.trim(),
            description: document.getElementById('description').value.trim(),
            startDate: document.getElementById('startDate').value || null,
            endDate: document.getElementById('endDate').value || null,
            price: parseFloat(document.getElementById('price').value || 0) || 0,
            maxParticipants: parseInt(document.getElementById('maxParticipants').value) || null,
            teacherId: (document.getElementById('teacherId').value) ? Number(document.getElementById('teacherId').value) : null,
            venueId: (document.getElementById('venueId').value) ? Number(document.getElementById('venueId').value) : null,
            status: 'PUBLISHED'
        };

        const btn = form.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Creating...';

        try {
            const res = await fetchJson(`${window.API_BASE_URL}/workshops`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            // Expect server returns created WorkshopDetailDTO
            const created = await safeParse(res);
            result.innerHTML = `<div class="success">Workshop created: <strong>${escapeHtml(created.title || created.workshopName || '—')}</strong> (id: ${created.id})</div>`;

            // Show groups panel
            createdWorkshopTitle.textContent = created.title || created.workshopName || '';
            createdWorkshopId.value = created.id;
            groupsSection.style.display = 'block';
            groupsList.innerHTML = '';
            if (Array.isArray(created.groups) && created.groups.length) renderGroups(created.groups);

            groupsSection.scrollIntoView({behavior:'smooth'});
        } catch (err) {
            console.error('create workshop error', err);
            // show user-friendly message
            result.innerHTML = `<div class="error">${escapeHtml(err.message || 'Failed to create')}</div>`;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Erstellen';
        }
    });

    resetBtn?.addEventListener('click', () => { form.reset(); });

    // === Create group inside workshop ===
    groupForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const workshopId = document.getElementById('createdWorkshopId').value;
        if (!workshopId) {
            alert('No workshop id found. Create workshop first.');
            return;
        }

        const groupPayload = {
            titleEn: document.getElementById('groupTitleEn').value.trim(),
            capacity: parseInt(document.getElementById('groupCapacity').value) || 10,
            startDateTime: document.getElementById('groupStart').value || null,
            endDateTime: document.getElementById('groupEnd').value || null,
            teacherId: document.getElementById('groupTeacherId').value ? Number(document.getElementById('groupTeacherId').value) : null,
            workshopId: Number(workshopId)
        };

        const btn = groupForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Adding...';

        try {
            const res = await fetchJson(`${window.API_BASE_URL}/groups`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(groupPayload)
            });

            const createdGroup = await safeParse(res);
            // prepend to UI
            prependGroupToList(createdGroup);
            groupForm.reset();
        } catch (err) {
            console.error('create group error', err);
            groupsList.insertAdjacentHTML('afterbegin', `<div class="error">Error: ${escapeHtml(err.message || 'Failed')}</div>`);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Gruppe hinzufügen';
        }
    });

    // === Helpers ===

    // fetchJson wrapper that attaches Authorization header and throws on 4xx/5xx with parsed message
    async function fetchJson(url, opts = {}) {
        const final = Object.assign({}, opts);
        final.headers = Object.assign({}, final.headers || {}, window.getAuthHeaders ? window.getAuthHeaders() : {});
        const res = await fetch(url, final);
        if (!res.ok) {
            // try parse meaningful error body
            const text = await res.text().catch(()=>'');
            let parsed;
            if (text) {
                try { parsed = JSON.parse(text); } catch { parsed = { message: text }; }
            }
            const msg = parsed?.message || parsed?.error || `HTTP ${res.status}`;
            const err = new Error(msg);
            err.status = res.status;
            throw err;
        }
        return res;
    }

    async function safeParse(res) {
        const ct = res.headers.get('content-type') || '';
        if (!ct.includes('application/json')) {
            const t = await res.text().catch(()=>'');
            try { return JSON.parse(t); } catch { return t; }
        }
        return res.json();
    }

    async function safeText(res) { try { return await res.text(); } catch { return ''; } }

    function prependGroupToList(g) {
        groupsList.insertAdjacentHTML('afterbegin', groupCardHtml(g));
    }
    function renderGroups(groups) {
        groupsList.innerHTML = groups.map(groupCardHtml).join('');
    }
    function groupCardHtml(g) {
        const title = escapeHtml(g.name || g.titleEn || g.title || 'Gruppe');
        const cap = g.capacity || 0;
        const start = g.startDateTime ? formatLocalDateTime(g.startDateTime) : '';
        const end = g.endDateTime ? formatLocalDateTime(g.endDateTime) : '';
        return `<div class="group-card" style="border:1px solid #eee;padding:0.6rem;margin-bottom:0.5rem;">
              <strong>${title}</strong>
              <div style="font-size:0.9rem;color:#555;">
                <div>${start} — ${end}</div>
                <div>Kapazität: ${cap}</div>
              </div>
            </div>`;
    }

    function escapeHtml(t){ const d=document.createElement('div'); d.textContent = t||''; return d.innerHTML; }
    function formatLocalDateTime(d){ if(!d) return ''; try { return new Date(d).toLocaleString('de-DE',{dateStyle:'medium',timeStyle:'short'});}catch{return d;} }
});