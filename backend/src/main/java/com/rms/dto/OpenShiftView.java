package com.rms.dto;

import com.rms.entity.OpenShiftStatus;

/** An open (claimable) shift as returned to workers/admins. */
public record OpenShiftView(
        Long id,
        Long clientId,
        String clientName,
        String roomNumber,
        String address,
        String location,
        String start,
        String end,
        int durationMinutes,
        OpenShiftStatus status,
        String notes,
        Long takenById,
        String takenByName,
        String takenAt,
        Long assignmentId
) {
}
