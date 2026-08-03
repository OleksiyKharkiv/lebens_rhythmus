package com.be.domain.exception;

/**
 * Thrown by AuthService.authenticate() when credentials are correct but the
 * account's email has never been confirmed via the verification link.
 * Deliberately a distinct type from BadCredentialsException so the frontend
 * can tell "wrong password" apart from "right password, unverified account"
 * and show an actionable message (e.g. offer to resend the verification
 * email) instead of a generic "invalid credentials".
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
