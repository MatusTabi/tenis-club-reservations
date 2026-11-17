package com.example.tenisclubreservations.reservation;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationE2ETest {
  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> request(String method, String path, String body) throws Exception {
    return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Content-Type", "application/json")
        .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void managesReservationsThroughHttp() throws Exception {
    var surface = request("POST", "/api/surfaces", "{\"name\":\"Reservation E2E\",\"pricePerMinute\":1.25}");
    assertEquals(201, surface.statusCode(), surface.body());
    var surfaceId = surface.headers().firstValue("Location").orElseThrow().replace("/api/surfaces/", "");
    var court = request("POST", "/api/courts", "{\"courtNumber\":901,\"surfaceId\":" + surfaceId + "}");
    assertEquals(201, court.statusCode(), court.body());
    var start = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.DAYS);
    String body = "{\"courtNumber\":901,\"startTime\":\"" + start + "\",\"endTime\":\"" + start.plus(60, ChronoUnit.MINUTES) + "\",\"gameType\":\"DOUBLES\",\"name\":\"Alice\",\"phoneNumber\":\"123456\"}";
    var created = request("POST", "/api/reservations", body);
    assertEquals(201, created.statusCode(), created.body());
    assertTrue(created.body().contains("112.50"), created.body());
    assertTrue(created.body().matches("(?s).*\\\"createdAt\\\":\\\"[^\\\"]+Z\\\".*"), created.body());
    var path = created.headers().firstValue("Location").orElseThrow();
    assertEquals(409, request("POST", "/api/reservations", body).statusCode());
    assertTrue(request("GET", "/api/courts/901/reservations", null).body().contains("Alice"));
    assertTrue(request("GET", "/api/reservations?phoneNumber=123456&futureOnly=true", null).body().contains("Alice"));
    assertEquals(200, request("PATCH", path, "{\"gameType\":\"SINGLES\"}").statusCode());
    assertEquals(400, request("PATCH", path, "{\"gameType\":null}").statusCode());
    assertEquals(409, request("DELETE", court.headers().firstValue("Location").orElseThrow(), null).statusCode());
    assertEquals(204, request("DELETE", path, null).statusCode());
    assertEquals(404, request("GET", path, null).statusCode());
  }

  @Test
  void listsWithOptionalPhoneAndFutureFilters() throws Exception {
    var surface = request("POST", "/api/surfaces", "{\"name\":\"All list HTTP\",\"pricePerMinute\":1}");
    String surfaceId = surface.headers().firstValue("Location").orElseThrow().replace("/api/surfaces/", "");
    assertEquals(201, request("POST", "/api/courts", "{\"courtNumber\":902,\"surfaceId\":" + surfaceId + "}").statusCode());
    Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(2, ChronoUnit.DAYS);
    String template = "{\"courtNumber\":902,\"startTime\":\"%s\",\"endTime\":\"%s\",\"gameType\":\"SINGLES\",\"name\":\"%s\",\"phoneNumber\":\"%s\"}";
    var past = request("POST", "/api/reservations", template.formatted(start, start.plusSeconds(600), "OldList", "555001"));
    var future = request("POST", "/api/reservations", template.formatted(start.plusSeconds(600), start.plusSeconds(1200), "NewList", "555002"));
    assertEquals(201, past.statusCode(), past.body());
    assertEquals(201, future.statusCode(), future.body());
    long pastId = Long.parseLong(past.headers().firstValue("Location").orElseThrow().replace("/api/reservations/", ""));
    jdbc.update("update reservation set start_time = timestamp with time zone '2020-01-01 10:00:00+00', end_time = timestamp with time zone '2020-01-01 10:10:00+00' where id = ?", pastId);

    var all = request("GET", "/api/reservations", null);
    assertEquals(200, all.statusCode());
    assertTrue(all.body().contains("OldList") && all.body().contains("NewList"));
    assertTrue(all.body().indexOf("NewList") < all.body().indexOf("OldList"));
    var allFuture = request("GET", "/api/reservations?futureOnly=true", null);
    assertTrue(allFuture.body().contains("NewList"));
    assertFalse(allFuture.body().contains("OldList"));
    var phone = request("GET", "/api/reservations?phoneNumber=555001", null);
    assertTrue(phone.body().contains("OldList"));
    assertFalse(phone.body().contains("NewList"));
    assertEquals("[]", request("GET", "/api/reservations?phoneNumber=555001&futureOnly=true", null).body());
    assertEquals("[]", request("GET", "/api/reservations?phoneNumber=not-found", null).body());
    assertEquals(400, request("GET", "/api/reservations?phoneNumber=%20%20", null).statusCode());
    assertEquals(400, request("GET", "/api/reservations?phoneNumber=", null).statusCode());
    assertEquals(204, request("DELETE", future.headers().firstValue("Location").orElseThrow(), null).statusCode());
    assertFalse(request("GET", "/api/reservations", null).body().contains("NewList"));
  }
}
