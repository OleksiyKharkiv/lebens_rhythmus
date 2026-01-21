// Corporate Page Script
// Handles dynamic loading and display of corporate programs

// API Configuration
const API_BASE_URL = 'http://localhost:3000/api';
const CORPORATE_ENDPOINT = `${API_BASE_URL}/corporate-programs`;

// DOM Elements
let servicesContainer;
let loadingIndicator;

// Initialize page
document.addEventListener('DOMContentLoaded', () => {
    initializePage();
});

/**
 * Initialize the corporate page
 */
function initializePage() {
    servicesContainer = document.querySelector('.services-container');

    // Create and add loading indicator if it doesn't exist
    if (!document.querySelector('.loading')) {
        loadingIndicator = document.createElement('div');
        loadingIndicator.className = 'loading';
        loadingIndicator.innerHTML = '<p>Lade Corporate Programme...</p>';
        loadingIndicator.style.display = 'none';
        servicesContainer.parentElement.appendChild(loadingIndicator);
    } else {
        loadingIndicator = document.querySelector('.loading');
    }

    loadCorporatePrograms();
}

/**
 * Fetch corporate programs from the API
 */
async function loadCorporatePrograms() {
    try {
        showLoading(true);

        const response = await fetch(CORPORATE_ENDPOINT);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const programs = await response.json();

        if (programs && programs.length > 0) {
            renderCorporatePrograms(programs);
        } else {
            // If no programs from API, keep the static content
            console.log('No corporate programs found, displaying static content');
        }

        showLoading(false);
    } catch (error) {
        console.error('Error loading corporate programs:', error);
        showLoading(false);
        // Keep static content on error
        showErrorMessage('Corporate Programme konnten nicht geladen werden. Die Standardprogramme werden angezeigt.');
    }
}

/**
 * Render corporate programs dynamically
 * @param {Array} programs - Array of corporate program objects
 */
function renderCorporatePrograms(programs) {
    // Clear existing content
    servicesContainer.innerHTML = '';

    programs.forEach(program => {
        const card = createProgramCard(program);
        servicesContainer.appendChild(card);
    });
}

/**
 * Create a program card element
 * @param {Object} program - Corporate program object
 * @returns {HTMLElement} - Program card element
 */
function createProgramCard(program) {
    const card = document.createElement('div');
    card.className = 'service-card';

    const title = document.createElement('h3');
    title.textContent = program.name || program.title;

    const description = document.createElement('p');
    description.textContent = program.description;

    card.appendChild(title);
    card.appendChild(description);

    // Add optional details if available
    if (program.duration || program.capacity) {
        const details = document.createElement('div');
        details.className = 'program-details';
        details.style.marginTop = '15px';
        details.style.fontSize = '0.95rem';
        details.style.color = '#888';

        if (program.duration) {
            const duration = document.createElement('p');
            duration.innerHTML = `<strong>Dauer:</strong> ${program.duration}`;
            details.appendChild(duration);
        }

        if (program.capacity) {
            const capacity = document.createElement('p');
            capacity.innerHTML = `<strong>Kapazität:</strong> ${program.capacity} Teilnehmer`;
            details.appendChild(capacity);
        }

        card.appendChild(details);
    }

    return card;
}

/**
 * Show or hide loading indicator
 * @param {boolean} show - Whether to show loading indicator
 */
function showLoading(show) {
    if (loadingIndicator) {
        loadingIndicator.style.display = show ? 'block' : 'none';
    }

    if (servicesContainer) {
        servicesContainer.style.opacity = show ? '0.5' : '1';
    }
}

/**
 * Display error message to user
 * @param {string} message - Error message to display
 */
function showErrorMessage(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.style.cssText = `
        background-color: #fff3cd;
        border: 1px solid #ffc107;
        color: #856404;
        padding: 15px;
        margin: 20px auto;
        max-width: 800px;
        border-radius: 8px;
        text-align: center;
    `;
    errorDiv.textContent = message;

    const servicesSection = document.querySelector('.services');
    if (servicesSection) {
        servicesSection.insertBefore(errorDiv, servicesContainer);

        // Remove error message after 5 seconds
        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }
}