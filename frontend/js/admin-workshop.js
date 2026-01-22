// frontend/js/admin-workshop.js
// Admin helper: create workshop + add groups to created workshop.
// Assumes window.API_BASE_URL and window.getAuthHeaders() available.

document.addEventListener('DOMContentLoaded', () => {
    // require auth and proper role
    if (!window.isAuthenticated || !window.isAuthenticated()) {
        location.href = '/pages/login/login.html';
        return;
    }

    // check role: allow only ADMIN/TEACHER/BUSINESS_OWNER to use this page
    let user = {};
    try { user = JSON.parse(localStorage.getItem('userData') || '{}'); } catch {}
    const allowed = ['ADMIN','TEACHER','BUSINESS_OWNER'];
    if (!allowed.includes((user.role || '').toUpperCase())) {
        document.body.innerHTML = '<main style="padding:2rem"><h2>Unauthorized</h2><p>Du hast keine Rechte für diese Seite.</p></main>';
        return;
    }

    const form = document.getElementById('createWorkshopForm');
    const result = document.getElementById('createResult');
    const resetBtn = document.getElementById('resetBtn');

    const groupsSection = document.getElementById('groupsSection');
    const createdWorkshopTitle = document.getElementById('createdWorkshopTitle');
    const createdWorkshopId = document.getElementById('createdWorkshopId');
    const groupsList = document.getElementById('groupsList');
    const groupForm = document.getElementById('createGroupForm');

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
            status: 'PUBLISHED' // admin creates published by default; change if needed
        };

        // disable button
        const btn = form.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Creating...';

        try {
            const res = await fetch(`${window.API_BASE_URL}/workshops`, {
                method: 'POST',
                headers: Object.assign({}, window.getAuthHeaders(), { 'Content-Type': 'application/json' }),
                body: JSON.stringify(payload)
            });

            const text = await safeText(res);
            let data = {};
            if (text) { try { data = JSON.parse(text);} catch { data = { message: text }; } }

            if (!res.ok) {
                throw new Error(data.message || `HTTP ${res.status}`);
            }

            // success: show created workshop and open groups section
            const created = data;
            result.innerHTML = `<div class="success">Workshop created: <strong>${escapeHtml(created.title || created.workshopName || '—')}</strong> (id: ${created.id})</div>`;
            // populate groups section
            createdWorkshopTitle.textContent = created.title || created.workshopName || '';
            createdWorkshopId.value = created.id;
            groupsSection.style.display = 'block';
            // clear groups list and populate if returned groups
            groupsList.innerHTML = '';
            if (Array.isArray(created.groups) && created.groups.length) {
                renderGroups(created.groups);
            }
            // scroll teams into view
            groupsSection.scrollIntoView({behavior:'smooth'});

        } catch (err) {
            console.error('create workshop error', err);
            result.innerHTML = `<div class="error">${escapeHtml(err.message || 'Failed')}</div>`;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Erstellen';
        }
    });

    resetBtn?.addEventListener('click', () => {
        form.reset();
    });

    // create group flow
    groupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const workshopId = document.getElementById('createdWorkshopId').value;
        if (!workshopId) {
            alert('No workshop id found. Create workshop first.');
            return;
        }

        const groupPayload = {
            // adjust body to match backend Group DTO - minimal fields:
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
            // NOTE: backend endpoint for creating groups — adapt if different.
            const res = await fetch(`${window.API_BASE_URL}/groups`, {
                method: 'POST',
                headers: Object.assign({}, window.getAuthHeaders(), { 'Content-Type': 'application/json' }),
                body: JSON.stringify(groupPayload)
            });

            const text = await safeText(res);
            let data = {};
            if (text) { try { data = JSON.parse(text); } catch { data = { message: text }; } }

            if (!res.ok) {
                throw new Error(data.message || `HTTP ${res.status}`);
            }

            // append created group to UI
            prependGroupToList(data);
            groupForm.reset();
        } catch (err) {
            console.error('create group error', err);
            groupsList.insertAdjacentHTML('afterbegin', `<div class="error">Error creating group: ${escapeHtml(err.message)}</div>`);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Gruppe hinzufügen';
        }
    });

    // Helpers

    function prependGroupToList(g) {
        const html = groupCardHtml(g);
        groupsList.insertAdjacentHTML('afterbegin', html);
    }

    function renderGroups(groups) {
        groupsList.innerHTML = groups.map(groupCardHtml).join('');
    }

    function groupCardHtml(g) {
        const title = escapeHtml(g.name || g.titleEn || g.title || 'Gruppe');
        const cap = g.capacity || g.capacityLeft || 0;
        const start = g.startDateTime ? formatLocalDateTime(g.startDateTime) : '';
        const end = g.endDateTime ? formatLocalDateTime(g.endDateTime) : '';
        return `
            <div class="group-card" style="border:1px solid #eee;padding:0.6rem;margin-bottom:0.5rem;">
                <strong>${title}</strong>
                <div style="font-size:0.9rem;color:#555;">
                    <div>${start} — ${end}</div>
                    <div>Kapazität: ${cap}</div>
                </div>
            </div>
        `;
    }

    // safe helpers
    async function safeText(res) {
        try { return await res.text(); } catch { return ''; }
    }
    function escapeHtml(t){ const d=document.createElement('div'); d.textContent = t||''; return d.innerHTML; }
    function formatLocalDateTime(d){ if(!d) return ''; try { return new Date(d).toLocaleString('de-DE',{dateStyle:'medium',timeStyle:'short'}); } catch { return d; } }
});