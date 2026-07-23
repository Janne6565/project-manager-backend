package com.janne6565.projectmanager.dto;

import com.janne6565.projectmanager.entities.AppUser;
import com.janne6565.projectmanager.entities.Role;

/** Public view of an authenticated user (never exposes the password hash). */
public record AuthUserResponse(String username, Role role) {

    public static AuthUserResponse from(AppUser user) {
        return new AuthUserResponse(user.getUsername(), user.getRole());
    }
}
