package com.platform.accident.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CaseLockedException extends RuntimeException {
    public CaseLockedException(String message) { super(message); }
}
