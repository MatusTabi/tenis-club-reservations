package com.example.tenisclubreservations.court;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;

public record CourtRequest(Map<String, Object> fields) {
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public CourtRequest {}
}
