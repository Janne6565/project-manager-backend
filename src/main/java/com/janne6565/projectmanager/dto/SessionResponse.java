package com.janne6565.projectmanager.dto;

import java.time.Instant;

/** A successful login/refresh: the access token, its absolute expiry, and the authenticated user. */
public record SessionResponse(String token, Instant expiresAt, AuthUserResponse user) {}
