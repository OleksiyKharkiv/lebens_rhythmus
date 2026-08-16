package com.be.domain.exception;

/**
 * Thrown by EnrollmentService when the user already has an Enrollment for
 * the requested Workshop/Course. Same reasoning as GroupFullException —
 * distinct type so the frontend can show a clean, expected-outcome message
 * instead of GlobalExceptionHandler's generic catch-all.
 */
public class AlreadyEnrolledException extends RuntimeException {
    public AlreadyEnrolledException(String message) {
        super(message);
    }
}
