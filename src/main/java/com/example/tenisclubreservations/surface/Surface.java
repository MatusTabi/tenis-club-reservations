package com.example.tenisclubreservations.surface;

import com.example.tenisclubreservations.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
@Entity
public class Surface extends BaseEntity {
  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 100, unique = true)
  private String activeName;

  @Column(nullable = false, precision = 6, scale = 2)
  private BigDecimal pricePerMinute;

  public void update(String name, String key, BigDecimal price) {
    this.name = name;
    this.activeName = key;
    this.pricePerMinute = price;
  }

  @Override
  public void softDelete() {
    super.softDelete();
    activeName = null;
  }
}
