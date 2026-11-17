package com.example.tenisclubreservations.reservation;

import com.example.tenisclubreservations.common.BaseEntity;
import com.example.tenisclubreservations.court.Court;
import com.example.tenisclubreservations.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

@Getter
@Entity
public class Reservation extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Court court;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Customer customer;

  @Column(nullable = false)
  private Instant startTime;

  @Column(nullable = false)
  private Instant endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GameType gameType;

  @Column(nullable = false, precision = 9, scale = 2)
  private BigDecimal totalPrice;

  public void update(Court court, Customer customer, Instant start, Instant end,
      GameType gameType, BigDecimal totalPrice) {
    this.court = court;
    this.customer = customer;
    this.startTime = start;
    this.endTime = end;
    this.gameType = gameType;
    this.totalPrice = totalPrice;
  }
}
