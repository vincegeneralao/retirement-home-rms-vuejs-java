package com.rms.dto;

/** Body of POST /api/admin/open-shifts/{openShiftId}/assign. Validated manually. */
public record AssignOpenShiftRequest(Long workerId) {
}
