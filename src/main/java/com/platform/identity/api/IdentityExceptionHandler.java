package com.platform.identity.api;

import com.platform.identity.exception.AccountLockedException;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.identity.exception.UserConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class IdentityExceptionHandler {

//    @ExceptionHandler(UserConflictException.class)
//    public ProblemDetail handleUserConflict(UserConflictException ex) {
//        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    @ExceptionHandler(com.platform.identity.exception.UnauthorizedException.class)
//    public ProblemDetail handleUnauthorized(Exception ex) {
//        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Access Denied");
//    }
    @ExceptionHandler(UserConflictException.class)
    public ProblemDetail handleUserConflict(UserConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Registration Conflict");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Security Violation");
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleLocked(AccountLockedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Security Block");
        return pd;
    }
}