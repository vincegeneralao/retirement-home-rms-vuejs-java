package com.rms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A shift slot published by an administrator that any support worker may claim.
 * Claiming (or admin-assigning) flips the status to TAKEN atomically and creates a
 * regular Assignment for the chosen worker.
 */
@Entity
@Table(name = "open_shifts",
        indexes = @Index(name = "idx_open_shifts_status_start", columnList = "status, scheduled_start"))
@Getter
@Setter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class OpenShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Read-only association used for projection of client details. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private Client client;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OpenShiftStatus status;

    /** Read-only association used for projection of the taker's name. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taken_by_id", insertable = false, updatable = false)
    private User takenBy;

    @Column(name = "taken_by_id")
    private Long takenById;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "assignment_id")
    private Long assignmentId;

    /** End of the slot = scheduled start + duration. */
    public LocalDateTime end() {
        return scheduledStart.plusMinutes(durationMinutes);
    }
}
