package com.platform.accident.media.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMediaTypeException extends RuntimeException {
    public InvalidMediaTypeException(String msg) { super(msg); }
}