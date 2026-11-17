package com.example.tenisclubreservations.court;

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
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {
  private final CourtService service;

  @GetMapping
  public List<CourtResponse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public CourtResponse get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<CourtResponse> create(@RequestBody CourtRequest input) {
    CourtResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/courts/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public CourtResponse put(@PathVariable long id, @RequestBody CourtRequest input) {
    return service.update(id, input, false);
  }

  @PatchMapping("/{id}")
  public CourtResponse patch(@PathVariable long id, @RequestBody CourtRequest input) {
    return service.update(id, input, true);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
