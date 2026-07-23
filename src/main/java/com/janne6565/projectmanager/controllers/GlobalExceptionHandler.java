package com.janne6565.projectmanager.controllers;

import com.janne6565.projectmanager.exceptions.BaseException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates {@link BaseException}s into RFC 7807 {@link ProblemDetail} responses. Only application
 * exceptions are handled here so existing default error handling for everything else is untouched;
 * stack traces and internal messages are never leaked to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ProblemDetail handleBase(BaseException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }
}
