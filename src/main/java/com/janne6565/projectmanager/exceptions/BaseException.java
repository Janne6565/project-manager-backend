package com.janne6565.projectmanager.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base type for all application exceptions. Carries the HTTP status to surface; {@link
 * com.janne6565.projectmanager.controllers.GlobalExceptionHandler} renders it as an RFC 7807 {@code
 * ProblemDetail}. Subclasses keep call sites intent-revealing.
 */
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;

    protected BaseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
