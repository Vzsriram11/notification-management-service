package com.schwab.notification.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Translates the small set of exceptions this API's services can throw
 * into the HTTP status (and body) a caller should actually see.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Without this, a lookup miss — e.g. GET /notifications/{id}/status for
     * an id that doesn't exist — surfaces as a 500 Internal Server Error
     * instead of the 404 the caller should get; Spring's default error
     * handling only maps unhandled exceptions to 500 unless something here
     * says otherwise.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Without this, a @Valid failure on the request body (e.g. an empty
     * recipientRefs, or a blank sourceSystem) still returns 400 by Spring's
     * own default, but with a bare {"status":400,"error":"Bad Request"}
     * body — Spring Boot suppresses field-level detail by default
     * (server.error.include-binding-errors defaults to "never", to avoid
     * leaking internal field names to a caller by accident). For an API
     * that other systems are meant to integrate against, telling the
     * caller *which* field was invalid and why is worth the trade-off, so
     * this handler assembles that detail explicitly instead of relying on
     * a global include-binding-errors setting that would apply everywhere.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", fieldErrors));
    }
}
