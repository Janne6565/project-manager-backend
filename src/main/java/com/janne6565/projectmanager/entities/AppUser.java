package com.janne6565.projectmanager.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A local user account (table {@code app_user}). Replaces the previous in-memory admin: password
 * login and OAuth login both resolve one of these and issue the app's own JWTs. The id is
 * app-assigned (never DB-generated); {@code passwordHash} is nullable because OAuth-only users have
 * no password.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    // Nullable: OAuth-only accounts (federated login) never set a password.
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AppUser(String username, String passwordHash, Role role) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
        this.createdAt = Instant.now();
    }
}
