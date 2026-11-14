package com.rms.entity;

/** Application roles. Persisted as its name (ADMIN / WORKER); exposed in JWT as ROLE_<name>. */
public enum Role {
    ADMIN,
    WORKER
}
