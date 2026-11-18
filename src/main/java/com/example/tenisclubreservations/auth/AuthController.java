package com.example.tenisclubreservations.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final TokenService tokens;

  @Operation(summary = "Log in using Basic credentials", security = @SecurityRequirement(name = "basicAuth"))
  @ApiResponse(responseCode = "200", description = "Token issued", headers = @Header(name = "Authorization", description = "Bearer JWT"))
  @ApiResponse(responseCode = "401", description = "Invalid Basic credentials")
  @PostMapping("/login")
  public ResponseEntity<Void> login(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header) {
    try {
      if (header == null || !header.startsWith("Basic ")) throw new IllegalArgumentException();
      String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
      int colon = decoded.indexOf(':');
      if (colon < 0) throw new IllegalArgumentException();
      return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.login(decoded.substring(0, colon), decoded.substring(colon + 1))).build();
    } catch (IllegalArgumentException | TokenService.InvalidToken ex) {
      return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"login\"").build();
    }
  }

  @Operation(summary = "Refresh a token in its refresh window", security = @SecurityRequirement(name = "bearerAuth"))
  @ApiResponse(responseCode = "200", description = "Token issued", headers = @Header(name = "Authorization", description = "Bearer JWT"))
  @ApiResponse(responseCode = "401", description = "Invalid bearer token")
  @ApiResponse(responseCode = "403", description = "Outside refresh window")
  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(Authentication authentication) {
    try {
      String token = tokens.refresh((TokenService.Identity) authentication.getPrincipal());
      return token == null ? ResponseEntity.status(403).build() : ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    } catch (TokenService.InvalidToken ex) {
      return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build();
    }
  }
}
