package com.platform.policy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PolicyNotFoundException extends RuntimeException {
    public PolicyNotFoundException(String id) { super("Policy declaration not found: " + id); }
}
