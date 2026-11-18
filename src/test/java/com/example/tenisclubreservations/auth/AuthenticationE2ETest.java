package com.example.tenisclubreservations.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.h2.console.enabled=true")
class AuthenticationE2ETest {
  @LocalServerPort int port;
  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> request(String method, String path, String authorization, String body) throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json");
    if (authorization != null) builder.header("Authorization", authorization);
    return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
  }

  private String login(String name, String password) throws Exception {
    String basic = Base64.getEncoder().encodeToString((name + ":" + password).getBytes(StandardCharsets.UTF_8));
    var response = request("POST", "/api/auth/login", "Basic " + basic, null);
    assertEquals(200, response.statusCode(), response.body());
    return response.headers().firstValue("Authorization").orElseThrow();
  }

  @Test
  void loginAuthorizationAndAccountLifecycle() throws Exception {
    var missing = request("GET", "/api/courts", null, null);
    assertEquals(401, missing.statusCode());
    assertEquals("Bearer", missing.headers().firstValue("WWW-Authenticate").orElseThrow());
    var badLogin = request("POST", "/api/auth/login", "Basic YWRtaW46d3Jvbmc=", null);
    assertEquals(401, badLogin.statusCode());
    assertTrue(badLogin.headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic"));
    assertEquals(401, request("GET", "/api/courts", "Bearer invalid", null).statusCode());
    assertEquals(200, request("GET", "/api/openapi", null, null).statusCode());
    assertTrue(request("GET", "/api/swagger-ui.html", null, null).statusCode() < 400);
    assertEquals(200, request("GET", "/api/swagger-ui/index.html", null, null).statusCode());
    assertEquals(200, request("GET", "/api/swagger-ui/swagger-ui-bundle.js", null, null).statusCode());
    assertEquals(200, request("GET", "/api/openapi/swagger-config", null, null).statusCode());
    assertTrue(request("GET", "/h2-console/", null, null).statusCode() >= 400);
    String admin = login("admin", "admin");
    assertEquals(400, request("POST", "/api/accounts", admin, "{\"username\":\"bad:name\",\"password\":\"secret\",\"role\":\"USER\"}").statusCode());
    assertEquals(403, request("POST", "/api/auth/refresh", admin, null).statusCode());
    var created = request("POST", "/api/accounts", admin, "{\"username\":\"alice\",\"password\":\"secret\",\"role\":\"USER\"}");
    assertEquals(201, created.statusCode(), created.body());
    assertFalse(created.body().contains("secret"));
    String path = created.headers().firstValue("Location").orElseThrow();
    assertEquals(400, request("PUT", path, admin, "{\"username\":\"bad:name\",\"password\":\"secret\",\"role\":\"USER\"}").statusCode());
    String user = login("alice", "secret");
    assertEquals(200, request("GET", "/api/courts", user, null).statusCode());
    var surface = request("POST", "/api/surfaces", admin, "{\"name\":\"Security E2E\",\"pricePerMinute\":1}");
    assertEquals(201, surface.statusCode());
    String surfaceId = surface.headers().firstValue("Location").orElseThrow().replace("/api/surfaces/", "");
    assertEquals(201, request("POST", "/api/courts", admin, "{\"courtNumber\":5432,\"surfaceId\":" + surfaceId + "}").statusCode());
    Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.DAYS);
    String reservation = "{\"courtNumber\":5432,\"startTime\":\"" + start + "\",\"endTime\":\"" + start.plusSeconds(3600) + "\",\"gameType\":\"SINGLES\",\"name\":\"Alice\",\"phoneNumber\":\"1234\"}";
    assertEquals(201, request("POST", "/api/reservations", user, reservation).statusCode());
    assertEquals(403, request("GET", "/api/accounts", user, null).statusCode());
    assertEquals(403, request("POST", "/api/courts", user, "{}").statusCode());
    assertEquals(200, request("PUT", path, admin, "{\"username\":\"alice\",\"password\":\"secret\",\"role\":\"ADMIN\"}").statusCode());
    assertEquals(200, request("GET", "/api/accounts", user, null).statusCode());
    assertEquals(200, request("PUT", path, admin, "{\"username\":\"alice\",\"password\":\"new:secret\",\"role\":\"USER\"}").statusCode());
    assertEquals(401, request("GET", "/api/courts", user, null).statusCode());
    user = login("alice", "new:secret");
    assertEquals(403, request("GET", "/api/accounts", user, null).statusCode());
    assertEquals(204, request("DELETE", path, admin, null).statusCode());
    assertEquals(401, request("GET", "/api/courts", user, null).statusCode());
    assertEquals(409, request("DELETE", "/api/accounts/1", admin, null).statusCode());
  }
}
