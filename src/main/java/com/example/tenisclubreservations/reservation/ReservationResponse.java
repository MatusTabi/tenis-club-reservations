package com.example.tenisclubreservations.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

public record ReservationResponse(Long id, int courtNumber, Instant startTime, Instant endTime,
    GameType gameType, String phoneNumber, String name, BigDecimal totalPrice,
    Instant createdAt, Instant updatedAt) {
  public static ReservationResponse from(Reservation reservation) {
    return new ReservationResponse(reservation.getId(), reservation.getCourt().getCourtNumber(),
        reservation.getStartTime(), reservation.getEndTime(), reservation.getGameType(),
        reservation.getCustomer().getPhoneNumber(), reservation.getCustomer().getName(),
        reservation.getTotalPrice(), reservation.getCreatedAt().toInstant(ZoneOffset.UTC),
        reservation.getUpdatedAt().toInstant(ZoneOffset.UTC));
  }
}
