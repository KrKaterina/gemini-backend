package com.platform.accident.submission.api;

import com.platform.accident.submission.exception.AccidentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccidentNotFoundException.class)
    public ProblemDetail handleNotFound(AccidentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidation(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrency(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The record was modified by another agent. Please refresh and try again.");
        pd.setTitle("Concurrency Conflict");
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleBusinessPolicyViolation(IllegalStateException ex) {
        // Maps business logic blocks to a clean 400 error for the UI
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Business Policy Violation");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}