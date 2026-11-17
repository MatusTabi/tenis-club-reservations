package com.example.tenisclubreservations.surface;

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

@Tag(name = "Surfaces")
@RestController
@RequestMapping("/api/surfaces")
@RequiredArgsConstructor
public class SurfaceController {
  private final SurfaceService service;

  @Operation(summary = "List active surfaces")
  @ApiResponse(responseCode = "200", description = "Surfaces returned")
  @GetMapping
  public List<SurfaceResponse> list() {
    return service.list();
  }

  @Operation(summary = "Get an active surface")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "200", description = "Surface returned")
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @GetMapping("/{id}")
  public SurfaceResponse get(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    return service.get(id);
  }

  @Operation(summary = "Create a surface")
  @ApiResponse(responseCode = "201", description = "Surface created; Location identifies it")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PostMapping
  public ResponseEntity<SurfaceResponse> create(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Surface.class))) @RequestBody SurfaceRequest input) {
    SurfaceResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/surfaces/" + result.id())).body(result);
  }

  @Operation(summary = "Replace a surface")
  @ApiResponse(responseCode = "200", description = "Surface updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PutMapping("/{id}")
  public SurfaceResponse put(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.Surface.class))) @RequestBody SurfaceRequest input) {
    return service.update(id, input, false);
  }

  @Operation(summary = "Partially update a surface", description = "Only supplied fields are changed; an empty object, null values and unknown fields are rejected.")
  @ApiResponse(responseCode = "200", description = "Surface updated")
  @ApiResponse(responseCode = "400", description = "Invalid request; business validation errors contain a message", content = @Content(mediaType = "application/json", schema = @Schema(anyOf = {RequestSchemas.ApiError.class, java.util.Map.class})))
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @PatchMapping("/{id}")
  public SurfaceResponse patch(@Parameter(description = "Resource ID", required = true) @PathVariable long id, @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestSchemas.SurfacePatch.class))) @RequestBody SurfaceRequest input) {
    return service.update(id, input, true);
  }

  @Operation(summary = "Delete a surface", description = "The record is soft-deleted.")
  @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.Map.class)))
  @ApiResponse(responseCode = "204", description = "Surface deleted", content = @Content)
  @ApiResponse(responseCode = "404", description = "Requested resource not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @ApiResponse(responseCode = "409", description = "Request conflicts with existing state", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSchemas.ApiError.class)))
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@Parameter(description = "Resource ID", required = true) @PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
