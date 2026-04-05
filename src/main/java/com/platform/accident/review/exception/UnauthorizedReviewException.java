package com.platform.accident.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedReviewException extends RuntimeException {
    public UnauthorizedReviewException(String message) { super(message); }
}
