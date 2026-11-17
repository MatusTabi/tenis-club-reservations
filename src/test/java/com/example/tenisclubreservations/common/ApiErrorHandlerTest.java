package com.example.tenisclubreservations.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ApiErrorHandlerTest {
  @Test
  void translatesUniqueViolationWithoutLeakingDetails() {
    var exception =
        new DataIntegrityViolationException("secret", new SQLException("secret", "23505"));
    var response = new ApiErrorHandler().handleConstraint(exception);
    assertEquals(409, response.getStatusCode().value());
    assertFalse(response.getBody().toString().contains("secret"));
  }
}
