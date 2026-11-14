package com.rms.dto;

/**
 * Body of PATCH /api/worker/assignments/{assignmentId}/status.
 *
 * Validation is performed in the service (not via bean-validation annotations) so that an
 * invalid status produces the API's "Request rejected" body with code VALIDATION, matching
 * the reference implementation. Allowed values: SCHEDULED, IN_PROGRESS, COMPLETED.
 */
public record UpdateStatusRequest(String status) {
}
