package com.example.tenisclubreservations.common;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class Input {
  private Input() {}

  public static void fields(Map<String, Object> input, Set<String> allowed, boolean partial) {
    if (input == null
        || input.isEmpty()
        || !allowed.containsAll(input.keySet())
        || (!partial && !input.keySet().containsAll(allowed))
        || input.values().stream().anyMatch(java.util.Objects::isNull)) {
      throw invalid("Missing, null, or unknown fields");
    }
  }

  public static String name(Object value) {
    if (!(value instanceof String name) || name.trim().isEmpty() || name.trim().length() > 50) {
      throw invalid("Name must contain 1 to 50 characters");
    }
    return name.trim();
  }

  public static BigDecimal price(Object value) {
    if (!(value instanceof Number)) {
      throw invalid("Price must be a number");
    }
    try {
      BigDecimal price = new BigDecimal(value.toString());
      if (price.scale() > 2
          || price.signum() < 0
          || price.compareTo(new BigDecimal("1000.00")) > 0) {
        throw invalid("Price must be between 0 and 1000 with at most two decimal places");
      }
      return price.setScale(2);
    } catch (NumberFormatException ex) {
      throw invalid("Invalid price");
    }
  }

  public static long positiveInteger(Object value) {
    if (!(value instanceof Number)) {
      throw invalid("Expected a positive integer");
    }
    try {
      long number = new BigDecimal(value.toString()).longValueExact();
      if (number <= 0) {
        throw invalid("Expected a positive integer");
      }
      return number;
    } catch (ArithmeticException | NumberFormatException ex) {
      throw invalid("Expected a positive integer");
    }
  }

  public static ApiException invalid(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }
}
