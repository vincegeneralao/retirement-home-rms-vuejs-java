package com.rms.error;

import lombok.Getter;

/**
 * A domain error that maps to a structured JSON problem body:
 * {@code {type, title:"Request rejected", status, detail, code}}. Thrown from services and
 * translated by {@link GlobalExceptionHandler}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;
    /** Optional seconds written into a {@code Retry-After} header (throttling errors). */
    private final Long retryAfterSeconds;

    public ApiException(String code, String message, int status) {
        this(code, message, status, null);
    }

    public ApiException(String code, String message, int status, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
