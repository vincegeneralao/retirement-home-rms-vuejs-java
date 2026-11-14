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
 * A support shift assigned to a worker for a client at a scheduled start.
 */
@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_worker_start", columnList = "worker_id, scheduled_start"),
        @Index(name = "idx_assignments_start", columnList = "scheduled_start")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Read-only association used only for projection of the worker's name. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", insertable = false, updatable = false)
    private User worker;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    /** Read-only association used only for projection of the client's details. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private Client client;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "scheduled_start", nullable = false)
    private LocalDateTime scheduledStart;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AssignmentStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    /** End of the shift = scheduled start + duration. */
    public LocalDateTime end() {
        return scheduledStart.plusMinutes(durationMinutes);
    }
}
