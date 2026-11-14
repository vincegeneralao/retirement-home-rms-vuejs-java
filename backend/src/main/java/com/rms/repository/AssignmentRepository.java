package com.rms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rms.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /** A worker's shifts within [from, to), with worker and client eagerly joined. */
    @Query("""
            select distinct a from Assignment a
            join fetch a.worker join fetch a.client
            where a.workerId = :workerId and a.scheduledStart >= :from and a.scheduledStart < :to
            order by a.scheduledStart asc
            """)
    List<Assignment> findForWorker(@Param("workerId") Long workerId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /** All shifts starting within [from, to), joined (admin day view / daily report). */
    @Query("""
            select distinct a from Assignment a
            join fetch a.worker join fetch a.client
            where a.scheduledStart >= :from and a.scheduledStart < :to
            order by a.scheduledStart asc
            """)
    List<Assignment> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** A single shift with its worker and client joined, for building a full view. */
    @Query("""
            select a from Assignment a
            join fetch a.worker join fetch a.client
            where a.id = :id
            """)
    Optional<Assignment> findWithRefsById(@Param("id") Long id);

    /** A worker's upcoming shifts (starting at/after {@code from}), soonest first, joined. */
    @Query("""
            select distinct a from Assignment a
            join fetch a.worker join fetch a.client
            where a.workerId = :workerId and a.scheduledStart >= :from
            order by a.scheduledStart asc
            """)
    List<Assignment> findUpcomingForWorker(@Param("workerId") Long workerId,
                                           @Param("from") LocalDateTime from);

    long countByWorkerIdAndScheduledStartBetween(Long workerId, LocalDateTime from, LocalDateTime to);

    /**
     * Candidate shifts used for overlap detection around a window. Associations are not needed,
     * so this returns plain entities; the caller filters to true overlaps in memory.
     */
    List<Assignment> findByWorkerIdAndScheduledStartBetween(Long workerId, LocalDateTime from, LocalDateTime to);
}
