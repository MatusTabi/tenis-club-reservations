package com.example.tenisclubreservations.court;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.surface.SurfaceRequest;
import com.example.tenisclubreservations.surface.SurfaceService;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CourtSurfaceIntegrationTest {
  @Autowired CourtService courts;
  @Autowired SurfaceService surfaces;
  @Autowired DataSource dataSource;

  @Test
  void liquibaseEnforcesUniqueActiveNumbers() throws Exception {
    var surface =
        surfaces.create(new SurfaceRequest(Map.of("name", "Constraint", "pricePerMinute", 1)));
    courts.create(new CourtRequest(Map.of("courtNumber", 777, "surfaceId", surface.id())));
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "insert into court (created_at, updated_at, court_number, active_number,"
                    + " surface_id) values (current_timestamp, current_timestamp, 777, 777, ? )")) {
      statement.setLong(1, surface.id());
      var exception = assertThrows(java.sql.SQLException.class, statement::executeUpdate);
      assertEquals("23505", exception.getSQLState());
    }
  }

  @Test
  void sortsAndAllowsReuseAfterSoftDeletion() {
    var z = surfaces.create(new SurfaceRequest(Map.of("name", "Zinc", "pricePerMinute", 0)));
    var a = surfaces.create(new SurfaceRequest(Map.of("name", "Alpine", "pricePerMinute", 1)));
    assertTrue(surfaces.list().indexOf(a) < surfaces.list().indexOf(z));
    var later = courts.create(new CourtRequest(Map.of("courtNumber", 250, "surfaceId", z.id())));
    var earlier = courts.create(new CourtRequest(Map.of("courtNumber", 200, "surfaceId", a.id())));
    assertTrue(courts.list().indexOf(earlier) < courts.list().indexOf(later));
    courts.delete(earlier.id());
    assertEquals(
        404,
        assertThrows(ApiException.class, () -> courts.get(earlier.id())).getStatusCode().value());
    assertDoesNotThrow(
        () -> courts.create(new CourtRequest(Map.of("courtNumber", 200, "surfaceId", a.id()))));
  }

  @Test
  void concurrentDuplicateNumbersAllowOnlyOneCreate() throws Exception {
    var surface =
        surfaces.create(new SurfaceRequest(Map.of("name", "Concurrent", "pricePerMinute", 1)));
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> task =
          () -> {
            start.await();
            try {
              courts.create(new CourtRequest(Map.of("courtNumber", 99, "surfaceId", surface.id())));
              return true;
            } catch (ApiException exception) {
              assertEquals(409, exception.getStatusCode().value());
              return false;
            }
          };
      var first = executor.submit(task);
      var second = executor.submit(task);
      start.countDown();
      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void concurrentAssignmentAndDeletionCannotLeaveActiveCourtOnDeletedSurface() throws Exception {
    var surface = surfaces.create(new SurfaceRequest(Map.of("name", "Race", "pricePerMinute", 1)));
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var create =
          executor.submit(
              () -> {
                start.await();
                try {
                  courts.create(
                      new CourtRequest(Map.of("courtNumber", 100, "surfaceId", surface.id())));
                  return true;
                } catch (ApiException ex) {
                  assertEquals(404, ex.getStatusCode().value());
                  return false;
                }
              });
      var delete =
          executor.submit(
              () -> {
                start.await();
                try {
                  surfaces.delete(surface.id());
                  return true;
                } catch (ApiException ex) {
                  assertEquals(409, ex.getStatusCode().value());
                  return false;
                }
              });
      start.countDown();
      assertNotEquals(create.get(10, TimeUnit.SECONDS), delete.get(10, TimeUnit.SECONDS));
    }
  }
}
