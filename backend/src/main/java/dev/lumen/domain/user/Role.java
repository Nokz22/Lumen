package dev.lumen.domain.user;

/**
 * CLINICIAN is documented (project-brief §3) but not implemented — a future read-only
 * role gated by explicit user consent, not by this enum alone.
 */
public enum Role {
    USER,
    ADMIN
}
