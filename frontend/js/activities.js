// Fetch activities from the backend
async function fetchActivities(filters = {}) {
    showLoading();
    hideNoResults();

    try {
        const queryParams = new URLSearchParams();

        if (filters.activityType) {
            queryParams.append('type', filters.activityType);
        }
        if (filters.ageGroup) {
            queryParams.append('ageGroup', filters.ageGroup);
        }
        if (filters.dayOfWeek) {
            queryParams.append('day', filters.dayOfWeek);
        }

        const url = `${window.API_BASE_URL}/activities${queryParams.toString() ? '?' + queryParams.toString() : ''}`;
        const activities = await window.fetchJson(url);
        hideLoading();

        if (!activities || activities.length === 0) {
            showNoResults();
        } else {
            renderActivities(activities);
        }
    } catch (error) {
        console.error('Error fetching activities:', error);
        hideLoading();
        showError('Fehler beim Laden der Aktivitäten. Bitte versuchen Sie es später erneut.');
    }
}

// Render activities to the DOM
function renderActivities(activities) {
    const container = document.getElementById('activities-container');
    container.innerHTML = '';

    activities.forEach(activity => {
        const activityCard = document.createElement('div');
        activityCard.className = 'activity-card';

        activityCard.innerHTML = `
            <div class="activity-image">
                <img src="${activity.imageUrl || '../../assets/placeholder.jpg'}" alt="${activity.name}">
            </div>
            <div class="activity-content">
                <h3>${activity.name}</h3>
                <p class="activity-description">${activity.description || 'Keine Beschreibung verfügbar'}</p>
                <div class="activity-details">
                    <span class="activity-type">${getActivityTypeLabel(activity.type)}</span>
                    <span class="activity-age">${getAgeGroupLabel(activity.ageGroup)}</span>
                </div>
                <div class="activity-schedule">
                    <span class="activity-day">${getDayLabel(activity.dayOfWeek)}</span>
                    <span class="activity-time">${activity.startTime || ''} - ${activity.endTime || ''}</span>
                </div>
                <div class="activity-footer">
                    <span class="activity-price">${activity.price ? activity.price + ' €' : 'Preis auf Anfrage'}</span>
                    <button class="primary" onclick="enrollActivity(${activity.id})">Anmelden</button>
                </div>
            </div>
        `;

        container.appendChild(activityCard);
    });
}

// Apply filters
function applyFilters() {
    const activityType = document.getElementById('activity-type').value;
    const ageGroup = document.getElementById('age-group').value;
    const dayOfWeek = document.getElementById('day-of-week').value;

    const filters = {
        activityType: activityType,
        ageGroup: ageGroup,
        dayOfWeek: dayOfWeek
    };

    fetchActivities(filters);
}

// Helper functions for loading state
function showLoading() {
    document.getElementById('loading').style.display = 'block';
    document.getElementById('activities-container').style.display = 'none';
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
    document.getElementById('activities-container').style.display = 'grid';
}

// Helper functions for no results state
function showNoResults() {
    document.getElementById('no-results').style.display = 'block';
    document.getElementById('activities-container').style.display = 'none';
}

function hideNoResults() {
    document.getElementById('no-results').style.display = 'none';
}

// Show error message
function showError(message) {
    const container = document.getElementById('activities-container');
    container.innerHTML = `
        <div class="error-message">
            <p>${message}</p>
        </div>
    `;
    container.style.display = 'block';
}

// Label mapping functions
function getActivityTypeLabel(type) {
    const labels = {
        'theater': 'Theater',
        'dance': 'Tanz',
        'gymnastics': 'Gymnastik',
        'music': 'Musik',
        'art': 'Kunst'
    };
    return labels[type] || type;
}

function getAgeGroupLabel(ageGroup) {
    const labels = {
        'children': 'Kinder (4-10)',
        'teens': 'Jugendliche (11-17)',
        'adults': 'Erwachsene (18+)'
    };
    return labels[ageGroup] || ageGroup;
}

function getDayLabel(day) {
    const labels = {
        'monday': 'Montag',
        'tuesday': 'Dienstag',
        'wednesday': 'Mittwoch',
        'thursday': 'Donnerstag',
        'friday': 'Freitag',
        'saturday': 'Samstag'
    };
    return labels[day] || day;
}

// Enroll in activity
function enrollActivity(activityId) {
    // Check if the user is logged in
    if (!window.isAuthenticated()) {
        alert('Bitte melden Sie sich an, um sich für eine Aktivität anzumelden.');
        window.location.href = '../login/login.html';
        return;
    }

    // Redirect to the enrollment page or show enrollment modal
    window.location.href = `../workshops/workshops.html?register=${activityId}`;
}

// Event listeners
document.addEventListener('DOMContentLoaded', function () {
    // Load all activities on page load
    fetchActivities();

    // Add filter button event listener
    document.getElementById('apply-filters').addEventListener('click', applyFilters);

    // Optional: Apply filters on select change
    document.getElementById('activity-type').addEventListener('change', applyFilters);
    document.getElementById('age-group').addEventListener('change', applyFilters);
    document.getElementById('day-of-week').addEventListener('change', applyFilters);
});