// API Configuration
const API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : 'https://api.tlab29.com/api';

// Get JWT token from localStorage
function getAuthToken() {
    return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
}

// Display success message
function showSuccessMessage(message) {
    const form = document.getElementById('feedbackForm');
    const successDiv = document.createElement('div');
    successDiv.className = 'alert alert-success';
    successDiv.textContent = message;
    successDiv.style.cssText = 'padding: 15px; margin: 20px 0; background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; border-radius: 4px;';

    form.parentNode.insertBefore(successDiv, form);

    setTimeout(() => {
        successDiv.remove();
    }, 5000);
}

// Display error message
function showErrorMessage(message) {
    const form = document.getElementById('feedbackForm');
    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-error';
    errorDiv.textContent = message;
    errorDiv.style.cssText = 'padding: 15px; margin: 20px 0; background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; border-radius: 4px;';

    form.parentNode.insertBefore(errorDiv, form);

    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

// Validate form inputs
function validateForm(formData) {
    const errors = [];

    if (!formData.feedbackType) {
        errors.push('Please select a feedback type');
    }

    if (!formData.subject || formData.subject.trim().length < 3) {
        errors.push('Subject must be at least 3 characters long');
    }

    if (!formData.message || formData.message.trim().length < 10) {
        errors.push('Message must be at least 10 characters long');
    }

    if (formData.email && !isValidEmail(formData.email)) {
        errors.push('Please enter a valid email address');
    }

    return errors;
}

// Validate email format
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Submit feedback to backend API
async function submitFeedback(feedbackData) {
    const token = getAuthToken();

    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/feedback`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(feedbackData),
            credentials: 'include'
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Server error: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error submitting feedback:', error);
        throw error;
    }
}

// Reset form after successful submission
function resetFeedbackForm() {
    const form = document.getElementById('feedbackForm');
    form.reset();
}

// Handle form submission
async function handleFormSubmit(event) {
    event.preventDefault();

    const form = event.target;
    const submitButton = form.querySelector('button[type="submit"]');

    // Collect form data
    const formData = {
        feedbackType: form.feedbackType.value,
        subject: form.subject.value.trim(),
        message: form.message.value.trim(),
        email: form.email.value.trim() || null,
        rating: form.rating.value ? parseInt(form.rating.value) : null
    };

    // Validate form
    const validationErrors = validateForm(formData);
    if (validationErrors.length > 0) {
        showErrorMessage(validationErrors.join('. '));
        return;
    }

    // Disable submit button
    submitButton.disabled = true;
    submitButton.textContent = 'Submitting...';

    try {
        await submitFeedback(formData);
        showSuccessMessage('Thank you for your feedback! We appreciate your input.');
        resetFeedbackForm();
    } catch (error) {
        showErrorMessage(error.message || 'Failed to submit feedback. Please try again later.');
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = 'Submit Feedback';
    }
}

// Initialize feedback form
function initializeFeedbackForm() {
    const form = document.getElementById('feedbackForm');

    if (form) {
        form.addEventListener('submit', handleFormSubmit);

        // Add character counter for message field
        const messageField = document.getElementById('message');
        if (messageField) {
            messageField.addEventListener('input', function () {
                const charCount = this.value.length;
                let counter = this.parentNode.querySelector('.char-counter');

                if (!counter) {
                    counter = document.createElement('div');
                    counter.className = 'char-counter';
                    counter.style.cssText = 'font-size: 0.85em; color: #666; margin-top: 5px;';
                    this.parentNode.appendChild(counter);
                }

                counter.textContent = `${charCount} characters`;
            });
        }
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', initializeFeedbackForm);