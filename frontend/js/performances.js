// Fetch performances from the backend
async function fetchPerformances() {
    try {
        return await window.fetchJson(`${window.API_BASE_URL}/performances`);
    } catch (error) {
        console.error('Error fetching performances:', error);
        throw error;
    }
}

// Render performance cards
function renderPerformances(performances) {
    const performancesList = document.querySelector('.performances-list');

    if (!performances || performances.length === 0) {
        performancesList.innerHTML = '<p class="no-performances">No performances are scheduled at the moment. Check back soon!</p>';
        return;
    }

    performancesList.innerHTML = performances.map(performance => `
        <article class="performance-card" data-performance-id="${performance.id}">
            <div class="performance-image">
                <img src="${performance.imageUrl || '../../assets/placeholder.jpg'}" 
                     alt="${performance.title || 'Performance'}"
                     onerror="this.src='../../assets/placeholder.jpg'">
            </div>
            <div class="performance-details">
                <h2 class="performance-title">${window.escapeHtml(performance.title || 'Untitled Performance')}</h2>
                <p class="performance-date">Date: ${window.formatLocalDate(performance.date)}</p>
                <p class="performance-location">Location: ${window.escapeHtml(performance.location || performance.venueName || 'TBD')}</p>
                <p class="performance-description">${window.escapeHtml(performance.description || 'Description coming soon.')}</p>
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