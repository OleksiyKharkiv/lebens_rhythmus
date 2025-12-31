const API_BASE_URL = '/api';

document.addEventListener('DOMContentLoaded', () => {
    loadWorkshops();
    loadWorkshopSelect();
    initRegistrationForm();
});

async function loadWorkshops() {
    try {
        const response = await fetch(`${API_BASE_URL}/workshops`);
        if (!response.ok) {
            throw new Error('Failed to fetch workshops');
        }

        const workshops = await response.json();
        displayWorkshops(workshops);
    } catch (error) {
        console.error('Error loading workshops:', error);
        displayError('workshops-content', 'Failed to load workshops. Please try again later.');
    }
}

function displayWorkshops(workshops) {
    const container = document.getElementById('workshops-content');

    if (!workshops || workshops.length === 0) {
        container.innerHTML = '<p class="no-workshops">No upcoming workshops available at the moment.</p>';
        return;
    }

    container.innerHTML = workshops.map(workshop => `
        <div class="workshop-item">
            <h3>${escapeHtml(workshop.title)}</h3>
            <p class="workshop-description">${escapeHtml(workshop.description || '')}</p>
            <div class="workshop-details">
                <p><strong>Date:</strong> ${formatDate(workshop.startDate)}</p>
                <p><strong>Duration:</strong> ${workshop.duration} minutes</p>
                <p><strong>Price:</strong> ${formatPrice(workshop.price)}</p>
                <p><strong>Location:</strong> ${escapeHtml(workshop.location || 'TBA')}</p>
                <p><strong>Available spots:</strong> ${workshop.maxParticipants - workshop.currentParticipants}/${workshop.maxParticipants}</p>
            </div>
        </div>
    `).join('');
}

async function loadWorkshopSelect() {
    try {
        const response = await fetch(`${API_BASE_URL}/workshops`);
        if (!response.ok) {
            throw new Error('Failed to fetch workshops');
        }

        const workshops = await response.json();
        populateWorkshopSelect(workshops);
    } catch (error) {
        console.error('Error loading workshop select:', error);
        displayError('workshop-select', 'Failed to load workshops for registration.');
    }
}

function populateWorkshopSelect(workshops) {
    const select = document.getElementById('workshop-select');

    const availableWorkshops = workshops.filter(w =>
        w.currentParticipants < w.maxParticipants && new Date(w.startDate) > new Date()
    );

    if (availableWorkshops.length === 0) {
        select.innerHTML = '<option value="">No workshops available for registration</option>';
        select.disabled = true;
        return;
    }

    select.innerHTML = '<option value="">Choose a workshop...</option>' +
        availableWorkshops.map(workshop => `
            <option value="${workshop.id}">
                ${escapeHtml(workshop.title)} - ${formatDate(workshop.startDate)}
            </option>
        `).join('');
}

function initRegistrationForm() {
    const form = document.getElementById('workshop-registration-form');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const formData = new FormData(form);
        const registrationData = {
            workshopId: formData.get('workshopId'),
            participantName: formData.get('participantName'),
            email: formData.get('email'),
            phone: formData.get('phone')
        };

        try {
            const response = await fetch(`${API_BASE_URL}/workshop-registrations`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(registrationData)
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Registration failed');
            }

            const result = await response.json();
            showSuccessMessage('Registration successful! You will receive a confirmation email shortly.');
            form.reset();
            loadWorkshops();
            loadWorkshopSelect();
        } catch (error) {
            console.error('Error submitting registration:', error);
            showErrorMessage(error.message || 'Failed to register. Please try again.');
        }
    });
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatPrice(price) {
    return new Intl.NumberFormat('de-DE', {
        style: 'currency',
        currency: 'EUR'
    }).format(price);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function displayError(elementId, message) {
    const element = document.getElementById(elementId);
    element.innerHTML = `<p class="error-message">${escapeHtml(message)}</p>`;
}

function showSuccessMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'success-message';
    messageDiv.textContent = message;
    document.querySelector('.workshop-registration').prepend(messageDiv);

    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}

function showErrorMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'error-message';
    messageDiv.textContent = message;
    document.querySelector('.workshop-registration').prepend(messageDiv);

    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}
