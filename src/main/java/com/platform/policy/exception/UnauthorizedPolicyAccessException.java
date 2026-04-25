package com.platform.policy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedPolicyAccessException extends RuntimeException {
    public UnauthorizedPolicyAccessException(String message) { super(message); }
}