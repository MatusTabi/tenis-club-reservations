package com.example.tenisclubreservations.court;

import com.example.tenisclubreservations.common.BaseEntity;
import com.example.tenisclubreservations.surface.Surface;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Getter
@Entity
public class Court extends BaseEntity {
  @Column(nullable = false)
  private int courtNumber;

  @Column(unique = true)
  private Integer activeNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Surface surface;

  public void update(int number, Surface surface) {
    this.courtNumber = number;
    this.activeNumber = number;
    this.surface = surface;
  }

  @Override
  public void softDelete() {
    super.softDelete();
    activeNumber = null;
  }
}
