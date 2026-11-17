package com.example.tenisclubreservations.reservation;

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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService service;

  @PostMapping("/reservations")
  public ResponseEntity<ReservationResponse> create(@RequestBody ReservationRequest input) {
    ReservationResponse result = service.create(input);
    return ResponseEntity.created(URI.create("/api/reservations/" + result.id())).body(result);
  }

  @GetMapping("/reservations/{id}")
  public ReservationResponse get(@PathVariable long id) {
    return service.get(id);
  }

  @GetMapping("/reservations")
  public List<ReservationResponse> byPhone(@RequestParam(required = false) String phoneNumber,
      @RequestParam(defaultValue = "false") boolean futureOnly) {
    return service.byPhone(phoneNumber, futureOnly);
  }

  @GetMapping("/courts/{courtNumber}/reservations")
  public List<ReservationResponse> byCourt(@PathVariable int courtNumber) {
    return service.byCourt(courtNumber);
  }

  @PutMapping("/reservations/{id}")
  public ReservationResponse put(@PathVariable long id, @RequestBody ReservationRequest input) {
    return service.update(id, input, false);
  }

  @PatchMapping("/reservations/{id}")
  public ReservationResponse patch(@PathVariable long id, @RequestBody ReservationRequest input) {
    return service.update(id, input, true);
  }

  @DeleteMapping("/reservations/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
