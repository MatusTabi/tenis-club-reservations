package com.example.tenisclubreservations.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "security.jwt.refresh-window=1h")
class RefreshE2ETest {
  @LocalServerPort int port;

  @Test
  void refreshesValidTokenOverHttp() throws Exception {
    var client = HttpClient.newHttpClient();
    var login = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
        .header("Authorization", "Basic YWRtaW46YWRtaW4=").POST(HttpRequest.BodyPublishers.noBody()).build();
    var response = client.send(login, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    var refresh = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/refresh"))
        .header("Authorization", response.headers().firstValue("Authorization").orElseThrow())
        .POST(HttpRequest.BodyPublishers.noBody()).build();
    var renewed = client.send(refresh, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, renewed.statusCode());
    assertTrue(renewed.headers().firstValue("Authorization").orElseThrow().startsWith("Bearer "));
  }
}
