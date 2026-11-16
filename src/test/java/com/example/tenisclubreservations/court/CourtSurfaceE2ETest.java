package com.example.tenisclubreservations.court;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CourtSurfaceE2ETest {
  @LocalServerPort int port;
  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> request(String method, String path, String body) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
    return client.send(
        builder
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void managesCourtAndSurfaceOverHttp() throws Exception {
    assertEquals(
        400,
        request("POST", "/api/surfaces", "{\"name\":\"Precise\",\"pricePerMinute\":1.230}")
            .statusCode());
    assertEquals(
        400,
        request(
                "POST",
                "/api/surfaces",
                "{\"name\":\"Precise\",\"pricePerMinute\":1000.00000000000001}")
            .statusCode());
    assertEquals(
        400,
        request(
                "POST",
                "/api/surfaces",
                "{\"name\":\"Precise\",\"pricePerMinute\":-0.00000000000001}")
            .statusCode());
    var surface = request("POST", "/api/surfaces", "{\"name\":\" Clay \",\"pricePerMinute\":1.25}");
    assertEquals(201, surface.statusCode(), surface.body());
    String surfacePath = surface.headers().firstValue("Location").orElseThrow();
    String surfaceId = surfacePath.substring(surfacePath.lastIndexOf('/') + 1);
    assertEquals(
        409,
        request("POST", "/api/surfaces", "{\"name\":\"clay\",\"pricePerMinute\":0}").statusCode());
    var court =
        request("POST", "/api/courts", "{\"courtNumber\":1,\"surfaceId\":" + surfaceId + "}");
    assertEquals(201, court.statusCode(), court.body());
    String courtPath = court.headers().firstValue("Location").orElseThrow();
    assertTrue(court.body().contains("Clay"));
    assertEquals(409, request("DELETE", surfacePath, null).statusCode());
    var secondSurface =
        request("POST", "/api/surfaces", "{\"name\":\"Hard\",\"pricePerMinute\":2}");
    String secondPath = secondSurface.headers().firstValue("Location").orElseThrow();
    String secondId = secondPath.substring(secondPath.lastIndexOf('/') + 1);
    assertEquals(200, request("PATCH", courtPath, "{\"surfaceId\":" + secondId + "}").statusCode());
    assertEquals(204, request("DELETE", surfacePath, null).statusCode());
    assertEquals(400, request("PATCH", courtPath, "{\"surfaceId\":null}").statusCode());
    assertEquals(200, request("PATCH", courtPath, "{\"courtNumber\":2}").statusCode());
    assertEquals(
        200, request("PUT", secondPath, "{\"name\":\"Grass\",\"pricePerMinute\":0}").statusCode());
    assertTrue(request("GET", "/api/courts", null).body().contains("Grass"));
    assertEquals(204, request("DELETE", courtPath, null).statusCode());
    assertEquals(404, request("GET", courtPath, null).statusCode());
    assertEquals(204, request("DELETE", secondPath, null).statusCode());
    assertEquals(404, request("GET", surfacePath, null).statusCode());
    assertEquals(
        201,
        request("POST", "/api/surfaces", "{\"name\":\"Grass\",\"pricePerMinute\":1000}")
            .statusCode());
  }
}
