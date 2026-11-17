package com.example.tenisclubreservations.reservation;

import static org.junit.jupiter.api.Assertions.*;

import com.example.tenisclubreservations.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationTest {
  @Test
  void calculatesAndRoundsPrices() {
    assertEquals(new BigDecimal("0.02"), ReservationService.price(new BigDecimal("0.01"), 1, GameType.DOUBLES));
    assertEquals(new BigDecimal("2699997.30"), ReservationService.price(new BigDecimal("9999.99"), 180, GameType.DOUBLES));
    assertEquals(new BigDecimal("1.25"), ReservationService.price(new BigDecimal("1.25"), 1, GameType.SINGLES));
  }

  @Test
  void acceptsOnlyExplicitOffsetMinuteAlignedTimes() {
    assertEquals(Instant.parse("2030-01-01T08:30:00Z"), ReservationService.time("2030-01-01T10:30:00+02:00"));
    assertThrows(ApiException.class, () -> ReservationService.time("2030-01-01T10:30:01Z"));
    assertThrows(ApiException.class, () -> ReservationService.time("2030-01-01T10:30:00+02:00:01"));
    assertThrows(ApiException.class, () -> ReservationService.time("2030-01-01T10:30:00"));
  }
}
