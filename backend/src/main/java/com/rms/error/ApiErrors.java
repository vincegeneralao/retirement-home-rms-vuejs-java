package com.rms.error;

/** Factories for every domain error code the API returns, mirroring the reference backend. */
public final class ApiErrors {

    private ApiErrors() {
    }

    public static ApiException invalidCredentials() {
        return new ApiException("INVALID_CREDENTIALS", "Invalid username or password", 401);
    }

    public static ApiException duplicateUsername(String username) {
        return new ApiException("DUPLICATE_USERNAME", "Username \"" + username + "\" is already taken", 409);
    }

    public static ApiException workerNotFound(Object id) {
        return id == null
                ? new ApiException("WORKER_NOT_FOUND", "Worker not found", 404)
                : new ApiException("WORKER_NOT_FOUND", "No worker with id " + id, 404);
    }

    public static ApiException availabilityConflict(String message) {
        return new ApiException("AVAILABILITY_CONFLICT", message, 409);
    }

    public static ApiException reportGenerationFailed(String message) {
        return new ApiException("REPORT_GENERATION_FAILED", message, 500);
    }

    public static ApiException clientNotFound(Object id) {
        return id == null
                ? new ApiException("CLIENT_NOT_FOUND", "Client not found", 404)
                : new ApiException("CLIENT_NOT_FOUND", "No client with id " + id, 404);
    }

    public static ApiException openShiftNotFound(Object id) {
        return id == null
                ? new ApiException("OPEN_SHIFT_NOT_FOUND", "Open shift not found", 404)
                : new ApiException("OPEN_SHIFT_NOT_FOUND", "No open shift with id " + id, 404);
    }

    public static ApiException shiftAlreadyTaken() {
        return new ApiException("SHIFT_ALREADY_TAKEN", "This shift has already been taken", 409);
    }

    public static ApiException shiftOverlap(String message) {
        return new ApiException("SHIFT_OVERLAP", message, 409);
    }

    public static ApiException invalidTimeRange() {
        return new ApiException("INVALID_TIME_RANGE", "endTime must be later than startTime", 400);
    }

    public static ApiException invalidWorker() {
        return new ApiException("INVALID_WORKER", "Worker is not an active support worker", 400);
    }

    public static ApiException assignmentNotFound(Object id) {
        return id == null
                ? new ApiException("ASSIGNMENT_NOT_FOUND", "Assignment not found", 404)
                : new ApiException("ASSIGNMENT_NOT_FOUND", "No assignment with id " + id, 404);
    }

    public static ApiException notYourAssignment() {
        return new ApiException("NOT_YOUR_ASSIGNMENT", "This shift belongs to another worker", 403);
    }

    public static ApiException shiftCancelled() {
        return new ApiException("SHIFT_CANCELLED", "This shift has been cancelled", 409);
    }

    public static ApiException validation(String message) {
        return new ApiException("VALIDATION", message, 400);
    }
}
