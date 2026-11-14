package com.rms.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Central translation of exceptions into the API's JSON bodies, matching the reference
 * backend exactly:
 *
 * <ul>
 *   <li>{@link ApiException} → {@code {type,title:"Request rejected",status,detail,code}}</li>
 *   <li>bean-validation failure → {@code {type,title:"Invalid request body",status,detail,errors{field:msg}}}</li>
 *   <li>anything else → {@code 500 {type,title:"Internal server error",...}}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BLANK = "about:blank";

    private static Map<String, Object> base(String title, int status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", BLANK);
        body.put("title", title);
        body.put("status", status);
        return body;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        Map<String, Object> body = base("Request rejected", ex.getStatus());
        body.put("detail", ex.getMessage());
        body.put("code", ex.getCode());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = base("Invalid request body", 400);
        body.put("detail", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleUnreadable(Exception ex) {
        Map<String, Object> body = base("Invalid request body", 400);
        body.put("detail", "Validation failed");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        Map<String, Object> body = base("Not found", HttpStatus.NOT_FOUND.value());
        body.put("detail", "Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        Map<String, Object> body = base("Internal server error", 500);
        body.put("detail", "An unexpected error occurred");
        return ResponseEntity.status(500).body(body);
    }
}
