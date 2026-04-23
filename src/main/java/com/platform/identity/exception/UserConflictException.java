package com.platform.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Triggered when a user tries to register with an existing email/username.
 * Signals HTTP 409 Conflict to the React Frontend.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserConflictException extends RuntimeException {
    public UserConflictException(String message) {
        super(message);
    }
}