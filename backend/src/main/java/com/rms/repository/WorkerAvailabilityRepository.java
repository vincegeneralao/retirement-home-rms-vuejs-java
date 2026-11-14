package com.rms.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.entity.DayOfWeek;
import com.rms.entity.WorkerAvailability;

public interface WorkerAvailabilityRepository extends JpaRepository<WorkerAvailability, Long> {

    List<WorkerAvailability> findByWorkerIdOrderByIdAsc(Long workerId);

    List<WorkerAvailability> findByWorkerIdInOrderByWorkerIdAscIdAsc(Collection<Long> workerIds);

    List<WorkerAvailability> findByWorkerIdAndDayOfWeek(Long workerId, DayOfWeek dayOfWeek);
}
