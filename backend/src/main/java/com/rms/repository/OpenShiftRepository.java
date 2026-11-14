package com.rms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rms.entity.OpenShift;
import com.rms.entity.OpenShiftStatus;

public interface OpenShiftRepository extends JpaRepository<OpenShift, Long> {

    /** All open shifts, newest-first, for the admin board. */
    @Query("""
            select distinct o from OpenShift o
            join fetch o.client left join fetch o.takenBy
            order by o.scheduledStart asc
            """)
    List<OpenShift> findAllJoined();

    /** Upcoming still-open shifts, for the worker claim board. */
    @Query("""
            select distinct o from OpenShift o
            join fetch o.client
            where o.status = :open and o.scheduledStart >= :from
            order by o.scheduledStart asc
            """)
    List<OpenShift> findOpenUpcoming(@Param("open") OpenShiftStatus open, @Param("from") LocalDateTime from);

    /** A single open shift with client (and taker, if taken) joined. */
    @Query("""
            select o from OpenShift o
            join fetch o.client left join fetch o.takenBy
            where o.id = :id
            """)
    Optional<OpenShift> findWithRefsById(@Param("id") Long id);

    /**
     * Atomically claim an open shift: flips it to TAKEN only while it is still OPEN. The
     * conditional update is the concurrency guard — a competing claim updates zero rows and the
     * caller turns that into a 409. {@code clearAutomatically} discards the stale entity so the
     * caller reloads the freshly-updated row.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OpenShift o
            set o.status = :newStatus, o.takenById = :workerId, o.takenAt = :now
            where o.id = :id and o.status = :expectedStatus
            """)
    int claim(@Param("id") Long id,
              @Param("workerId") Long workerId,
              @Param("now") LocalDateTime now,
              @Param("expectedStatus") OpenShiftStatus expectedStatus,
              @Param("newStatus") OpenShiftStatus newStatus);
}
