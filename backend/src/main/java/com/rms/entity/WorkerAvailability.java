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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A weekly availability window (e.g. MONDAY 08:00-12:00) declared for a worker.
 * Windows for the same worker/day must not overlap.
 */
@Entity
@Table(name = "worker_availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_availability_worker_day_start",
                columnNames = {"worker_id", "day_of_week", "start_time"}),
        indexes = @Index(name = "idx_availability_worker", columnList = "worker_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkerAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Read-only association (worker is referenced by {@link #workerId}). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", insertable = false, updatable = false)
    private User worker;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", length = 5, nullable = false)
    private String startTime;

    @Column(name = "end_time", length = 5, nullable = false)
    private String endTime;

    /**
     * True when this window overlaps another on the same day. Times are compared as
     * zero-padded "HH:mm" strings, which sort identically to their true ordering.
     */
    public boolean overlaps(WorkerAvailability other) {
        return this.dayOfWeek == other.dayOfWeek
                && this.startTime.compareTo(other.endTime) < 0
                && other.startTime.compareTo(this.endTime) < 0;
    }
}
