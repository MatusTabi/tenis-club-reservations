package com.example.tenisclubreservations.surface;

import java.math.BigDecimal;

public record SurfaceResponse(Long id, String name, BigDecimal pricePerMinute) {
  public static SurfaceResponse from(Surface surface) {
    return new SurfaceResponse(surface.getId(), surface.getName(), surface.getPricePerMinute());
  }
}
