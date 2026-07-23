package com.janne6565.projectmanager.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 403 — an OAuth login was authenticated by the provider but the user is not entitled to the app
 * (none of the required {@code project-manager-*} groups). Surfaced to the frontend as {@code
 * oauthError=noAccess}.
 */
public class OAuthAccessDeniedException extends BaseException {
    public OAuthAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
