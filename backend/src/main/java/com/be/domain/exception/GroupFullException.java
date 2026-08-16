package com.be.domain.exception;

/**
 * Thrown by EnrollmentService when a Group's atomic capacity check
 * (GroupRepository.decrementCapacityLeft) returns 0 rows affected. Distinct
 * from a plain RuntimeException (found by architect-reviewer, LR-084 round
 * 3) so the frontend can show a clean "this course/workshop is full"
 * message instead of GlobalExceptionHandler's generic catch-all — for
 * Course specifically, this is the everyday full-course path, not a rare
 * race condition.
 */
public class GroupFullException extends RuntimeException {
    public GroupFullException(String message) {
        super(message);
    }
}
