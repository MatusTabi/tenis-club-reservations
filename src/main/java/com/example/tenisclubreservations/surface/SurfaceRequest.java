package com.example.tenisclubreservations.surface;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;

public record SurfaceRequest(Map<String, Object> fields) {
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public SurfaceRequest {}
}
