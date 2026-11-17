package com.example.tenisclubreservations.common;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, String>> handle(ApiException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(Map.of("message", exception.getReason()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleConstraint(
      DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
        return ResponseEntity.status(409)
            .body(Map.of("message", "A record with that value already exists"));
      }
      cause = cause.getCause();
    }
    return ResponseEntity.internalServerError().body(Map.of("message", "Unable to save record"));
  }
}
