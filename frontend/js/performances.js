// API Configuration
const API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : 'https://api.tlab29.com/api';

// Fetch performances from backend
async function fetchPerformances() {
    try {
        const response = await fetch(`${API_BASE_URL}/performances`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const performances = await response.json();
        return performances;
    } catch (error) {
        console.error('Error fetching performances:', error);
        throw error;
    }
}

// Format date for display
function formatDate(dateString) {
    if (!dateString) return 'TBD';

    const date = new Date(dateString);
    const options = {year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'};
    return date.toLocaleDateString('en-US', options);
}

// Render performance cards
function renderPerformances(performances) {
    const performancesList = document.querySelector('.performances-list');

    if (!performances || performances.length === 0) {
        performancesList.innerHTML = '<p class="no-performances">No performances scheduled at the moment. Check back soon!</p>';
        return;
    }

    performancesList.innerHTML = performances.map(performance => `
        <article class="performance-card" data-performance-id="${performance.id}">
            <div class="performance-image">
                <img src="${performance.imageUrl || '../../assets/images/performance-placeholder.jpg'}" 
                     alt="${performance.title || 'Performance'}"
                     onerror="this.src='../../assets/images/performance-placeholder.jpg'">
            </div>
            <div class="performance-details">
                <h2 class="performance-title">${performance.title || 'Untitled Performance'}</h2>
                <p class="performance-date">Date: ${formatDate(performance.date)}</p>
                <p class="performance-location">Location: ${performance.location || 'TBD'}</p>
                <p class="performance-description">${performance.description || 'Description coming soon.'}</p>
                ${performance.ticketUrl
        ? `<a href="${performance.ticketUrl}" class="btn btn-primary" target="_blank" rel="noopener noreferrer">Get Tickets</a>`
        : `<a href="#" class="btn btn-primary" onclick="showPerformanceDetails(${performance.id}); return false;">Learn More</a>`
    }
            </div>
        </article>
    `).join('');
}

// Show loading state
function showLoading() {
    const performancesList = document.querySelector('.performances-list');
    performancesList.innerHTML = `
        <div class="loading-spinner">
            <p>Loading performances...</p>
        </div>
    `;
}

// Handle errors
function handleError(error) {
    const performancesList = document.querySelector('.performances-list');
    performancesList.innerHTML = `
        <div class="error-message">
            <p>Sorry, we couldn't load the performances at this time.</p>
            <p>Please try again later or contact us if the problem persists.</p>
            <button class="btn btn-secondary" onclick="initPerformancesPage()">Retry</button>
        </div>
    `;
    console.error('Performance loading error:', error);
}

// Show performance details (can be expanded to show modal with more info)
function showPerformanceDetails(performanceId) {
    console.log('Showing details for performance:', performanceId);
    // This can be expanded to show a modal or navigate to a detail page
    alert('Performance details will be available soon!');
}

// Initialize the performances page
async function initPerformancesPage() {
    showLoading();

    try {
        const performances = await fetchPerformances();
        renderPerformances(performances);
    } catch (error) {
        handleError(error);
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', initPerformancesPage);