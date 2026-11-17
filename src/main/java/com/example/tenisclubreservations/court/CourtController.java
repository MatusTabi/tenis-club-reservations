package com.example.tenisclubreservations.court;

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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Courts")
@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {
  private final CourtService service;

  @Operation(summary = "List active courts")
  @ApiResponse(responseCode = "200", description = "Courts returned")
  @GetMapping
  public List<CourtResponse> list() {
    return service.list();
  }

  @Operation(summary = "Get an active court")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "200", description = "Court returned")
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @GetMapping("/{id}")
  public CourtResponse get(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    return service.get(id);
  }

  @Operation(summary = "Create a court")
  @ApiResponse(responseCode = "201", description = "Court created; Location identifies it")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PostMapping
  public ResponseEntity<CourtResponse> create(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Court.class))) @RequestBody CourtRequest input) {
    CourtResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/courts/" + result.id())).body(result);
  }

  @Operation(summary = "Replace a court")
  @ApiResponse(responseCode = "200", description = "Court updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PutMapping("/{id}")
  public CourtResponse put(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Court.class))) @RequestBody CourtRequest input) {
    return service.update(id, input, false);
  }

  @Operation(summary = "Partially update a court", description = "Only supplied fields are changed; an empty object, null values and unknown fields are rejected.")
  @ApiResponse(responseCode = "200", description = "Court updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PatchMapping("/{id}")
  public CourtResponse patch(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.CourtPatch.class))) @RequestBody CourtRequest input) {
    return service.update(id, input, true);
  }

  @Operation(summary = "Delete a court", description = "The record is soft-deleted.")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "204", description = "Court deleted", content = @Content)
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
