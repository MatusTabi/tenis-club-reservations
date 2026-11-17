package com.example.tenisclubreservations.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InputTest {
  @Test
  void validatesPrices() {
    assertEquals(new BigDecimal("1000.00"), Input.price(new BigDecimal("1000.00")));
    assertEquals(new BigDecimal("0.00"), Input.price(0));
    assertThrows(ApiException.class, () -> Input.price(new BigDecimal("0.001")));
    assertThrows(ApiException.class, () -> Input.price(-1));
    assertThrows(ApiException.class, () -> Input.price(1001));
  }

  @Test
  void validatesPatchFields() {
    assertDoesNotThrow(() -> Input.fields(Map.of("name", "Clay"), Set.of("name", "price"), true));
    assertThrows(ApiException.class, () -> Input.fields(Map.of(), Set.of("name"), true));
    Map<String, Object> input = new HashMap<>();
    input.put("name", null);
    assertThrows(ApiException.class, () -> Input.fields(input, Set.of("name"), true));
    assertThrows(ApiException.class, () -> Input.fields(Map.of("other", 1), Set.of("name"), true));
  }
}
