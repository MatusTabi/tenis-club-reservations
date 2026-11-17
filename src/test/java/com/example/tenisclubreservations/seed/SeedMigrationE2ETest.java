package com.example.tenisclubreservations.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SeedMigrationE2ETest {
  @Test
  void startsWithAndWithoutSeedEnvironmentVariable() throws Exception {
    checkStartup(false);
    checkStartup(true);
  }

  private void checkStartup(boolean enabled) throws Exception {
    int port;
    try (var socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    var builder = new ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp", System.getProperty("java.class.path"),
        "com.example.tenisclubreservations.TenisClubReservationsApplication",
        "--server.port=" + port);
    builder.environment().remove("SEED_DATA_ENABLED");
    if (enabled) {
      builder.environment().put("SEED_DATA_ENABLED", "true");
    }
    builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    builder.redirectErrorStream(true);
    Process process = builder.start();
    try {
      var client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
      String courts = getWhenReady(client, port, "/api/courts", process);
      String surfaces = getWhenReady(client, port, "/api/surfaces", process);
      if (enabled) {
        assertTrue(courts.contains("Clay") && courts.contains("Hard"), courts);
        assertTrue(surfaces.contains("3.60") && surfaces.contains("5.00"), surfaces);
      } else {
        assertEquals("[]", courts);
        assertEquals("[]", surfaces);
      }
    } finally {
      process.destroy();
      if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly();
        process.waitFor();
      }
    }
  }

  private String getWhenReady(HttpClient client, int port, String path, Process process)
      throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .timeout(Duration.ofSeconds(2)).GET().build();
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline && process.isAlive()) {
      try {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
      } catch (java.io.IOException e) {
        Thread.sleep(100);
      }
    }
    throw new AssertionError("Application did not start on port " + port);
  }
}
