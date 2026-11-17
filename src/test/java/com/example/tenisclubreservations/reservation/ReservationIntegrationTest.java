package com.example.tenisclubreservations.reservation;

import static org.junit.jupiter.api.Assertions.*;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.court.CourtRequest;
import com.example.tenisclubreservations.court.CourtService;
import com.example.tenisclubreservations.surface.SurfaceRequest;
import com.example.tenisclubreservations.surface.SurfaceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ReservationIntegrationTest {
  @Autowired ReservationService reservations;
  @Autowired CourtService courts;
  @Autowired SurfaceService surfaces;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transaction;

  private Map<String, Object> input(int court, Instant start, int minutes, String phone) {
    return new HashMap<>(Map.of("courtNumber", court, "startTime", start.toString(), "endTime", start.plus(minutes, ChronoUnit.MINUTES).toString(), "gameType", "DOUBLES", "name", "Alice", "phoneNumber", phone));
  }

  private Instant future() { return Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(2, ChronoUnit.DAYS); }

  @Test
  void lifecyclePricingAvailabilityCustomerAndCourtRules() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "BookingSurface", "pricePerMinute", 1.25)));
    var court = courts.create(new CourtRequest(Map.of("courtNumber", 811, "surfaceId", surface.id())));
    var start = future();
    var first = reservations.create(new ReservationRequest(input(811, start, 60, " 123 ")));
    assertEquals(new BigDecimal("112.50"), first.totalPrice());
    assertEquals("123", first.phoneNumber());
    assertEquals(409, assertThrows(ApiException.class, () -> reservations.create(new ReservationRequest(input(811, start.plusSeconds(60), 60, "other")))).getStatusCode().value());
    var adjacent = reservations.create(new ReservationRequest(input(811, start.plusSeconds(3600), 30, "123")));
    assertEquals(adjacent.id(), reservations.byCourt(811).getFirst().id());
    assertEquals(2, reservations.byPhone("123", true).size());
    var mismatch = input(811, start.plusSeconds(7200), 30, "123");
    mismatch.put("name", "Bob");
    assertEquals(409, assertThrows(ApiException.class, () -> reservations.create(new ReservationRequest(mismatch))).getStatusCode().value());
    assertEquals(409, assertThrows(ApiException.class, () -> courts.delete(court.id())).getStatusCode().value());
    surfaces.update(surface.id(), new SurfaceRequest(Map.of("pricePerMinute", 2)), true);
    var customerOnly = reservations.update(first.id(), new ReservationRequest(Map.of("phoneNumber", "555", "name", "Bob")), true);
    assertEquals(new BigDecimal("112.50"), customerOnly.totalPrice());
    var changed = reservations.update(first.id(), new ReservationRequest(Map.of("gameType", "SINGLES")), true);
    assertEquals(new BigDecimal("120.00"), changed.totalPrice());
    assertEquals(400, assertThrows(ApiException.class, () -> reservations.update(first.id(), new ReservationRequest(Map.of("phoneNumber", "3")), true)).getStatusCode().value());
    reservations.delete(first.id());
    assertEquals(404, assertThrows(ApiException.class, () -> reservations.get(first.id())).getStatusCode().value());
    reservations.create(new ReservationRequest(input(811, start, 60, "123")));
  }

  @Test
  void concurrentOverlappingRequestsCannotBothSucceed() throws Exception {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "BookingRace", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 812, "surfaceId", surface.id())));
    var request = new ReservationRequest(input(812, future(), 60, "race"));
    var latch = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Boolean> task = () -> {
        latch.await();
        try { reservations.create(request); return true; }
        catch (ApiException e) { assertEquals(409, e.getStatusCode().value()); return false; }
      };
      var a = executor.submit(task); var b = executor.submit(task); latch.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void boundariesValidationAndRollback() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "BoundarySurface", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 813, "surfaceId", surface.id())));
    var start = future();
    for (int duration : new int[] {0, 181, -1}) {
      assertEquals(400, assertThrows(ApiException.class, () -> reservations.create(new ReservationRequest(input(813, start, duration, "invalid")))).getStatusCode().value());
    }
    var one = reservations.create(new ReservationRequest(input(813, start, 1, "bound")));
    reservations.create(new ReservationRequest(input(813, start.plusSeconds(60), 180, "bound")));
    assertEquals(409, assertThrows(ApiException.class, () -> reservations.create(new ReservationRequest(input(813, start, 1, "never-created")))).getStatusCode().value());
    assertEquals(0, jdbc.queryForObject("select count(*) from customer where phone_number = 'never-created'", Integer.class));
    assertEquals(400, assertThrows(ApiException.class, () -> reservations.update(one.id(), new ReservationRequest(Map.of("gameType", "SINGLES")), false)).getStatusCode().value());
    assertEquals(400, assertThrows(ApiException.class, () -> reservations.update(one.id(), new ReservationRequest(Map.of("unknown", 1)), true)).getStatusCode().value());
    assertEquals(813, reservations.update(one.id(), new ReservationRequest(input(813, start, 1, "bound")), false).courtNumber());
  }

  @Test
  void historicalReadsRenumberReuseAndFutureFilter() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "HistorySurface", "pricePerMinute", 1)));
    var court = courts.create(new CourtRequest(Map.of("courtNumber", 814, "surfaceId", surface.id())));
    var a = reservations.create(new ReservationRequest(input(814, future(), 1, "history")));
    var b = reservations.create(new ReservationRequest(input(814, future().plusSeconds(60), 1, "history")));
    jdbc.update("update reservation set created_at = timestamp '2020-01-01 00:00:00' where id in (?, ?)", a.id(), b.id());
    assertEquals(b.id(), reservations.byPhone("history", false).getFirst().id());
    courts.update(court.id(), new CourtRequest(Map.of("courtNumber", 815)), true);
    assertEquals(2, reservations.byCourt(815).size());
    assertEquals(404, assertThrows(ApiException.class, () -> reservations.byCourt(814)).getStatusCode().value());
    jdbc.update("update reservation set start_time = timestamp with time zone '2020-01-01 10:00:00+00', end_time = timestamp with time zone '2020-01-01 11:00:00+00' where id in (?, ?)", a.id(), b.id());
    assertTrue(reservations.byPhone("history", true).isEmpty());
    assertEquals(409, assertThrows(ApiException.class, () -> reservations.delete(a.id())).getStatusCode().value());
    assertEquals(409, assertThrows(ApiException.class, () -> reservations.update(a.id(), new ReservationRequest(Map.of("gameType", "SINGLES")), true)).getStatusCode().value());
    courts.delete(court.id());
    courts.create(new CourtRequest(Map.of("courtNumber", 815, "surfaceId", surface.id())));
    assertEquals(815, reservations.get(a.id()).courtNumber());
    assertEquals(2, reservations.byPhone("history", false).size());
    assertTrue(reservations.byCourt(815).isEmpty());
  }

  @Test
  void storesLargestDatabaseRatePrice() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "MaxPriceSurface", "pricePerMinute", 1000)));
    courts.create(new CourtRequest(Map.of("courtNumber", 816, "surfaceId", surface.id())));
    jdbc.update("update surface set price_per_minute = 9999.99 where id = ?", surface.id());
    var created = reservations.create(new ReservationRequest(input(816, future(), 180, "max-price")));
    assertEquals(new BigDecimal("2699997.30"), reservations.get(created.id()).totalPrice());
  }

  @Test
  void simultaneousCustomerCreationUsesOneCustomer() throws Exception {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "CustomerRaceSurface", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 817, "surfaceId", surface.id())));
    var latch = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> { latch.await(); return reservations.create(new ReservationRequest(input(817, future(), 30, "same-race"))); });
      var second = executor.submit(() -> { latch.await(); return reservations.create(new ReservationRequest(input(817, future().plusSeconds(3600), 30, "same-race"))); });
      latch.countDown();
      first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
    }
    assertEquals(1, jdbc.queryForObject("select count(*) from customer where phone_number = 'same-race'", Integer.class));
    assertEquals(2, reservations.byPhone("same-race", false).size());
  }

  @Test
  void courtDeletionAndReservationCreationCannotBothSucceed() throws Exception {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "DeleteRaceSurface", "pricePerMinute", 1)));
    var court = courts.create(new CourtRequest(Map.of("courtNumber", 818, "surfaceId", surface.id())));
    var latch = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var create = executor.submit(() -> { latch.await(); try { reservations.create(new ReservationRequest(input(818, future(), 30, "delete-race"))); return true; } catch (ApiException e) { assertEquals(404, e.getStatusCode().value()); return false; } });
      var delete = executor.submit(() -> { latch.await(); try { courts.delete(court.id()); return true; } catch (ApiException e) { assertEquals(409, e.getStatusCode().value()); return false; } });
      latch.countDown();
      assertNotEquals(create.get(10, TimeUnit.SECONDS), delete.get(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void rateAndCourtMutationRacesProduceConsistentPrice() throws Exception {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "RateRaceSurface", "pricePerMinute", 1)));
    var other = surfaces.create(new SurfaceRequest(Map.of("name", "OtherRaceSurface", "pricePerMinute", 3)));
    var court = courts.create(new CourtRequest(Map.of("courtNumber", 819, "surfaceId", surface.id())));
    var latch = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(3)) {
      var create = executor.submit(() -> { latch.await(); return reservations.create(new ReservationRequest(input(819, future(), 60, "rate-race"))); });
      var changeRate = executor.submit(() -> { latch.await(); return surfaces.update(surface.id(), new SurfaceRequest(Map.of("pricePerMinute", 2)), true); });
      var changeCourt = executor.submit(() -> { latch.await(); return courts.update(court.id(), new CourtRequest(Map.of("surfaceId", other.id())), true); });
      latch.countDown();
      var price = create.get(10, TimeUnit.SECONDS).totalPrice();
      changeRate.get(10, TimeUnit.SECONDS); changeCourt.get(10, TimeUnit.SECONDS);
      assertTrue(java.util.Set.of(new BigDecimal("90.00"), new BigDecimal("180.00"), new BigDecimal("270.00")).contains(price), price.toString());
    }
  }

  @Test
  void failureAfterCustomerAndReservationWritesRollsBackBoth() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "RollbackSurface", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 820, "surfaceId", surface.id())));
    assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
      reservations.create(new ReservationRequest(input(820, future(), 30, "rollback-phone")));
      assertEquals(1, jdbc.queryForObject("select count(*) from customer where phone_number = 'rollback-phone'", Integer.class));
      assertEquals(1, jdbc.queryForObject("select count(*) from reservation r join customer c on r.customer_id = c.id where c.phone_number = 'rollback-phone'", Integer.class));
      throw new IllegalStateException("force rollback after both writes");
    }));
    assertEquals(0, jdbc.queryForObject("select count(*) from customer where phone_number = 'rollback-phone'", Integer.class));
    assertTrue(reservations.byPhone("rollback-phone", false).isEmpty());
  }

  @Test
  void listsAllOrByPhoneWithOptionalFutureFilterAndStableOrder() {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "AllListSurface", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 821, "surfaceId", surface.id())));
    var start = future();
    var past = reservations.create(new ReservationRequest(input(821, start, 10, "list-a")));
    var first = reservations.create(new ReservationRequest(input(821, start.plusSeconds(600), 10, "list-a")));
    var second = reservations.create(new ReservationRequest(input(821, start.plusSeconds(1200), 10, "list-b")));
    var deleted = reservations.create(new ReservationRequest(input(821, start.plusSeconds(1800), 10, "list-a")));
    reservations.delete(deleted.id());
    jdbc.update("update reservation set created_at = timestamp '2099-01-01 00:00:00' where id in (?, ?, ?)", past.id(), first.id(), second.id());
    jdbc.update("update reservation set start_time = timestamp with time zone '2020-01-01 10:00:00+00', end_time = timestamp with time zone '2020-01-01 10:10:00+00' where id = ?", past.id());

    var all = reservations.byPhone(null, false).stream().map(ReservationResponse::id).toList();
    var allFuture = reservations.byPhone(null, true).stream().map(ReservationResponse::id).toList();
    assertTrue(all.indexOf(second.id()) < all.indexOf(first.id()));
    assertTrue(all.indexOf(first.id()) < all.indexOf(past.id()));
    assertFalse(all.contains(deleted.id()));
    assertTrue(allFuture.contains(first.id()));
    assertTrue(allFuture.contains(second.id()));
    assertFalse(allFuture.contains(past.id()));
    assertEquals(java.util.List.of(first.id(), past.id()), reservations.byPhone("list-a", false).stream().map(ReservationResponse::id).toList());
    assertEquals(java.util.List.of(first.id()), reservations.byPhone("list-a", true).stream().map(ReservationResponse::id).toList());
    assertTrue(reservations.byPhone("missing-list-phone", false).isEmpty());
    assertEquals(400, assertThrows(ApiException.class, () -> reservations.byPhone("  ", false)).getStatusCode().value());
  }
}
