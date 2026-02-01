
// Submit feedback to backend API
async function submitFeedback(feedbackData) {
    try {
        return await window.fetchJson(`${window.API_BASE_URL}/feedbacks`, {
            method: 'POST',
            body: JSON.stringify(feedbackData)
        });
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

// Initialize a feedback form
function initializeFeedbackForm() {
    const form = document.getElementById('feedbackForm');

    if (form) {
        form.addEventListener('submit', handleFormSubmit);

        // Add character counter for the message field
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