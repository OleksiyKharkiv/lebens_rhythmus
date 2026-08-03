package com.be.domain.exception;

/** Thrown when a /verify-email link's token doesn't match any stored hash, or has expired. */
public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}
