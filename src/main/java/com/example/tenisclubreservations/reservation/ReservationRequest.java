package com.example.tenisclubreservations.reservation;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationRequest {
  private final Map<String, Object> fields = new LinkedHashMap<>();

  public ReservationRequest() {}

  public ReservationRequest(Map<String, Object> fields) {
    this.fields.putAll(fields);
  }

  @JsonAnySetter
  public void put(String key, Object value) {
    fields.put(key, value);
  }

  public Map<String, Object> fields() {
    return fields;
  }
}
