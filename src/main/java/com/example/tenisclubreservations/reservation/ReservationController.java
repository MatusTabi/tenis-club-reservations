package com.example.tenisclubreservations.reservation;

import com.example.tenisclubreservations.apidoc.RequestSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservations")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService service;

  @Operation(summary = "Create a reservation")
  @ApiResponse(responseCode = "201", description = "Reservation created; Location identifies it")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PostMapping("/reservations")
  public ResponseEntity<ReservationResponse> create(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Reservation.class))) @RequestBody ReservationRequest input) {
    ReservationResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/reservations/" + result.id())).body(result);
  }

  @Operation(summary = "Get an active reservation")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "200", description = "Reservation returned")
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @GetMapping("/reservations/{id}")
  public ReservationResponse get(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    return service.get(id);
  }

  @Operation(summary = "List active reservations", description = "Omit phoneNumber to list all active reservations. Set futureOnly to true to include only reservations starting in the future. Results are ordered by creation time descending, then ID descending.")
  @ApiResponse(responseCode = "200", description = "Reservations returned")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @GetMapping("/reservations")
  public List<ReservationResponse> byPhone(@Parameter(description = "Optional nonblank customer phone number") @RequestParam(required = false) String phoneNumber,
      @Parameter(description = "Only reservations starting in the future") @RequestParam(defaultValue = "false") boolean futureOnly) {
    return service.byPhone(phoneNumber, futureOnly);
  }

  @Operation(summary = "List reservations for a court")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "200", description = "Reservations returned")
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @GetMapping("/courts/{courtNumber}/reservations")
  public List<ReservationResponse> byCourt(@Parameter(description = "Court number", required = true) @PathVariable int courtNumber) {
    return service.byCourt(courtNumber);
  }

  @Operation(summary = "Replace a reservation")
  @ApiResponse(responseCode = "200", description = "Reservation updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PutMapping("/reservations/{id}")
  public ReservationResponse put(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Reservation.class))) @RequestBody ReservationRequest input) {
    return service.update(id, input, false);
  }

  @Operation(summary = "Partially update a reservation", description = "Only supplied fields are changed; an empty object, null values and unknown fields are rejected.")
  @ApiResponse(responseCode = "200", description = "Reservation updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PatchMapping("/reservations/{id}")
  public ReservationResponse patch(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.ReservationPatch.class))) @RequestBody ReservationRequest input) {
    return service.update(id, input, true);
  }

  @Operation(summary = "Delete a future reservation", description = "The record is soft-deleted.")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "204", description = "Reservation deleted", content = @Content)
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @DeleteMapping("/reservations/{id}")
  public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
