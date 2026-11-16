package com.example.tenisclubreservations.surface;

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

@RestController
@RequestMapping("/api/surfaces")
@RequiredArgsConstructor
public class SurfaceController {
  private final SurfaceService service;

  @GetMapping
  public List<SurfaceResponse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public SurfaceResponse get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<SurfaceResponse> create(@RequestBody SurfaceRequest input) {
    SurfaceResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/surfaces/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public SurfaceResponse put(@PathVariable long id, @RequestBody SurfaceRequest input) {
    return service.update(id, input, false);
  }

  @PatchMapping("/{id}")
  public SurfaceResponse patch(@PathVariable long id, @RequestBody SurfaceRequest input) {
    return service.update(id, input, true);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
