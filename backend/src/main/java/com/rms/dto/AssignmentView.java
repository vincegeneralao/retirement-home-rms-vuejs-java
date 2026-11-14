package com.rms.dto;

import com.rms.entity.AssignmentStatus;

/** A scheduled shift as returned to worker/admin dashboards and reports. */
public record AssignmentView(
        Long id,
        Long workerId,
        String workerName,
        Long clientId,
        String clientName,
        String roomNumber,
        String address,
        String location,
        String start,
        String end,
        int durationMinutes,
        AssignmentStatus status,
        String notes
) {
}
