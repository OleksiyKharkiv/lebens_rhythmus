// frontend/js/dashboard.js
document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('authToken');
    if (!token) {
        location.href = '../login/login.html';
        return;
    }

    try {
        const profileRes = await fetch(`${window.API_BASE_URL}/users/me`, {headers: window.getAuthHeaders()});
        if (!profileRes.ok) throw new Error('Failed to fetch profile');
        const profile = await profileRes.json();
        document.querySelector('.userName').textContent = profile.firstName || profile.email || 'Gast';

        const enrollRes = await fetch(`${window.API_BASE_URL}/users/me/enrollments`, {headers: window.getAuthHeaders()});
        if (!enrollRes.ok) throw new Error('Failed to fetch enrollments');
        const enrollments = await enrollRes.json();
        renderMyCourses(enrollments);
    } catch (err) {
        console.error(err);
        document.getElementById('myCoursesList').innerHTML = '<li>Fehler beim Laden</li>';
    }
});

function renderMyCourses(enrollments) {
    const list = document.getElementById('myCoursesList');
    if (!enrollments || enrollments.length === 0) {
        list.innerHTML = '<li>Keine Kurse</li>';
        return;
    }
    list.innerHTML = enrollments.map(e => `
    <li>
      <span class="course-title">${escapeHtml(e.workshopTitle)}</span>
      <div class="course-meta">
        <span class="status">${e.status}</span>
        <span class="date">${e.createdAt ? new Date(e.createdAt).toLocaleDateString('de-DE') : ''}</span>
      </div>
    </li>
  `).join('');
}

function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t || '';
    return d.innerHTML;
}