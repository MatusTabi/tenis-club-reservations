package com.example.tenisclubreservations.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Documentation-only models for the map-backed request DTOs. */
public final class RequestSchemas {
  private RequestSchemas() {}

  @Schema(description = "Business-rule error response")
  public record ApiError(String message) {}

  @Schema(description = "All fields are required; unknown or null fields are rejected.",
      requiredProperties = {"courtNumber", "surfaceId"}, additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
  public record Court(@Schema(description = "Positive court number", minimum = "1", maximum = "2147483647") Integer courtNumber,
                      @Schema(description = "ID of an existing surface", minimum = "1") Long surfaceId) {}

  @Schema(description = "At least one field is required; null and unknown fields are rejected.",
      additionalProperties = Schema.AdditionalPropertiesValue.FALSE, minProperties = 1)
  public record CourtPatch(@Schema(minimum = "1", maximum = "2147483647") Integer courtNumber,
                           @Schema(description = "ID of an existing surface", minimum = "1") Long surfaceId) {}

  @Schema(description = "All fields are required; unknown or null fields are rejected.",
      requiredProperties = {"name", "pricePerMinute"}, additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
  public record Surface(@Schema(description = "Nonblank name, up to 50 characters after trimming") String name,
                        @Schema(description = "CZK per minute; at most two decimal places", minimum = "0", maximum = "1000") BigDecimal pricePerMinute) {}

  @Schema(description = "At least one field is required; null and unknown fields are rejected.",
      additionalProperties = Schema.AdditionalPropertiesValue.FALSE, minProperties = 1)
  public record SurfacePatch(@Schema(description = "Nonblank name, up to 50 characters after trimming") String name,
                             @Schema(description = "CZK per minute; at most two decimal places", minimum = "0", maximum = "1000") BigDecimal pricePerMinute) {}

  @Schema(description = "All fields are required; unknown or null fields are rejected. Timestamps must have an offset and be minute-aligned; the reservation must start in the future and last 1–180 minutes.",
      requiredProperties = {"courtNumber", "startTime", "endTime", "gameType", "phoneNumber", "name"}, additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
  public record Reservation(@Schema(minimum = "1", maximum = "2147483647") Integer courtNumber,
                            @Schema(format = "date-time") String startTime,
                            @Schema(format = "date-time") String endTime,
                            @Schema(allowableValues = {"SINGLES", "DOUBLES"}) String gameType,
                            @Schema(description = "Nonblank phone number, up to 100 characters after trimming") String phoneNumber,
                            @Schema(description = "Nonblank customer name, up to 50 characters after trimming") String name) {}

  @Schema(description = "At least one field is required. If supplied, phoneNumber and name must be supplied together. Null and unknown fields are rejected. Timestamps must have an offset and be minute-aligned; the resulting reservation must start in the future and last 1–180 minutes.",
      additionalProperties = Schema.AdditionalPropertiesValue.FALSE, minProperties = 1)
  public record ReservationPatch(@Schema(minimum = "1", maximum = "2147483647") Integer courtNumber,
                                 @Schema(format = "date-time") String startTime,
                                 @Schema(format = "date-time") String endTime,
                                 @Schema(allowableValues = {"SINGLES", "DOUBLES"}) String gameType,
                                 @Schema(description = "Nonblank phone number, up to 100 characters after trimming") String phoneNumber,
                                 @Schema(description = "Nonblank customer name, up to 50 characters after trimming") String name) {}
}
