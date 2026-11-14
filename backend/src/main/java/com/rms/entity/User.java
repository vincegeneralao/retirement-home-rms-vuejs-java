package com.rms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A system user: either an administrator or a support worker.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 80, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Bumped whenever previously issued tokens must stop working (e.g. deactivation). A JWT
     * carries the value it was minted with, and every guarded request re-checks it, so old
     * tokens die immediately instead of living out their full TTL.
     */
    @Builder.Default
    @Column(name = "token_version", nullable = false, columnDefinition = "integer not null default 0")
    private int tokenVersion = 0;

    /**
     * Set when an administrator resets the password to the blank default. The worker is then
     * prompted to pick a new password (or explicitly keep the blank one) on their next login.
     */
    @Builder.Default
    @Column(name = "password_reset_required", nullable = false, columnDefinition = "boolean not null default false")
    private boolean passwordResetRequired = false;
}
