package com.janne6565.projectmanager.entities;

/**
 * Fixed role hierarchy: {@code OWNER} ▸ {@code ADMIN} ▸ {@code USER}. A higher role implicitly
 * satisfies any lower one via {@link #isAtLeast(Role)}. Persisted as a STRING on {@link AppUser}.
 */
public enum Role {
    USER,
    ADMIN,
    OWNER;

    /** True when this role is the given role or ranks above it. */
    public boolean isAtLeast(Role other) {
        return ordinal() >= other.ordinal();
    }
}
