package com.platform.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Triggered when the Identity Registry denies a capability (hasPermission returns false).
 * Signals HTTP 401/403 to the React Frontend.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
    public UnauthorizedException() {
        super("Access Denied: Insufficient Permissions");
    }
}