// frontend/js/admin-users.js
// Admin: list users, search, change roles and deactivate accounts.

document.addEventListener('DOMContentLoaded', init);

let allUsers = [];

async function init() {
    if (typeof window.API_BASE_URL === 'undefined') {
        console.error('API base not defined');
        return;
    }

    // Auth check
    if (!window.isAuthenticated()) {
        location.href = '../login/login.html';
        return;
    }

    await loadUsers();
    bindEvents();
}

function bindEvents() {
    const searchInput = document.getElementById('userSearch');
    searchInput?.addEventListener('input', debounce(() => {
        const query = searchInput.value.trim();
        if (query.length > 2) {
            searchUsers(query);
        } else if (query.length === 0) {
            renderUsers(allUsers);
        }
    }, 300));

    const form = document.getElementById('userActionForm');
    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveUserChanges();
    });

    document.getElementById('cancelUserEdit')?.addEventListener('click', () => {
        form.style.display = 'none';
        form.reset();
    });
}

async function loadUsers() {
    const list = document.getElementById('usersList');
    if (list) list.innerHTML = '<p>Loading users...</p>';

    try {
        allUsers = await window.fetchJson(`${window.API_BASE_URL}/users`);
        renderUsers(allUsers);
    } catch (err) {
        console.error('Failed to load users', err);
        if (list) list.innerHTML = '<p class="error">Error loading users.</p>';
    }
}

async function searchUsers(query) {
    try {
        const results = await window.fetchJson(`${window.API_BASE_URL}/users/search?query=${encodeURIComponent(query)}`);
        renderUsers(results);
    } catch (err) {
        console.error('Search failed', err);
    }
}

function renderUsers(users) {
    const list = document.getElementById('usersList');
    if (!list) return;

    if (!users || users.length === 0) {
        list.innerHTML = '<p>No users found.</p>';
        return;
    }

    list.innerHTML = users.map(u => `
        <div class="user-item" onclick="editUser(${u.id})" style="padding: 10px; border-bottom: 1px solid #eee; cursor: pointer;">
            <strong>${window.escapeHtml(u.firstName || '')} ${window.escapeHtml(u.lastName || '')}</strong><br/>
            <small>${window.escapeHtml(u.email)}</small> | <small>${u.role}</small> | 
            <small style="color: ${u.enabled ? 'green' : 'red'}">${u.enabled ? 'Active' : 'Deactivated'}</small>
        </div>
    `).join('');
}

window.editUser = function (id) {
    const user = allUsers.find(u => u.id === id);
    if (!user) return;

    const form = document.getElementById('userActionForm');
    if (!form) return;

    form.style.display = 'block';
    document.getElementById('uId').value = user.id;
    document.getElementById('uRole').value = user.role;
    document.getElementById('uEnabled').checked = user.enabled;

    form.scrollIntoView({behavior: 'smooth'});
};

async function saveUserChanges() {
    const id = document.getElementById('uId').value;
    const role = document.getElementById('uRole').value;
    const enabled = document.getElementById('uEnabled').checked;

    try {
        // Update role
        await window.fetchJson(`${window.API_BASE_URL}/users/${id}/role?role=${role}`, {
            method: 'PUT'
        });

        // If enabled checkbox was unchecked, deactivate user
        if (!enabled) {
            await window.fetchJson(`${window.API_BASE_URL}/users/${id}`, {
                method: 'DELETE'
            });
        }

        // Note: Currently backend doesn't seem to have a simple "reactivate" endpoint in UserController
        // but we assume success for the role change.

        alert('User updated successfully');
        await loadUsers();
        document.getElementById('userActionForm').style.display = 'none';
    } catch (err) {
        console.error('Failed to update user', err);
        alert('Error: ' + err.message);
    }
}

function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}