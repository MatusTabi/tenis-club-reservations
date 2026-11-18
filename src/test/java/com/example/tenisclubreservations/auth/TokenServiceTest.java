package com.example.tenisclubreservations.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.tenisclubreservations.account.AccountDao;
import com.example.tenisclubreservations.account.UserAccount;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.Optional;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TokenServiceTest {
  private final AccountDao dao = mock(AccountDao.class);
  private final String secret = "test-only-signing-secret-at-least-32-bytes";
  private final TokenService tokens = new TokenService(dao, new BCryptPasswordEncoder(), secret, Duration.ofHours(1), Duration.ofMinutes(15));

  @Test
  void validatesConfiguration() {
    assertThrows(IllegalArgumentException.class, () -> new TokenService(dao, new BCryptPasswordEncoder(), "short", Duration.ofHours(1), Duration.ofMinutes(15)));
    assertThrows(IllegalArgumentException.class, () -> new TokenService(dao, new BCryptPasswordEncoder(), secret, Duration.ofMinutes(1), Duration.ofMinutes(2)));
  }

  @Test
  void rejectsInvalidTokens() {
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate("not-a-token"));
  }

  @Test
  void loginRejectsPasswordsOverBcryptByteLimit() {
    var account = mock(UserAccount.class);
    var encoder = new BCryptPasswordEncoder();
    var service = new TokenService(dao, encoder, secret, Duration.ofHours(1), Duration.ofMinutes(15));
    when(dao.byUsername("ascii")).thenReturn(Optional.of(account));
    when(account.getId()).thenReturn(1L);
    when(account.getPasswordHash()).thenReturn(encoder.encode("x".repeat(72)));
    assertNotNull(service.login("ascii", "x".repeat(72)));
    assertThrows(TokenService.InvalidToken.class, () -> service.login("ascii", "x".repeat(73)));
    when(account.getPasswordHash()).thenReturn(encoder.encode("é".repeat(36)));
    assertNotNull(service.login("ascii", "é".repeat(36)));
    assertThrows(TokenService.InvalidToken.class, () -> service.login("ascii", "é".repeat(36) + "x"));
  }

  @Test
  void rejectsExpiredAndDeletedAccountTokens() throws Exception {
    Instant now = Instant.parse("2026-11-17T00:00:00Z");
    tokens.setClock(Clock.fixed(now, ZoneOffset.UTC));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(signed(now)));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(signed(now.plusSeconds(60))));
  }

  @Test
  void rejectsWrongSignatureMissingClaimsAndVersionMismatch() throws Exception {
    Instant now = Instant.parse("2026-11-17T00:00:00Z");
    tokens.setClock(Clock.fixed(now, ZoneOffset.UTC));
    var claims = new JWTClaimsSet.Builder().subject("1").expirationTime(Date.from(now.plusSeconds(60))).claim("version", 0).build();
    var wrong = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    wrong.sign(new MACSigner("a-different-signing-secret-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(wrong.serialize()));
    var missing = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder().subject("1").expirationTime(Date.from(now.plusSeconds(60))).build());
    missing.sign(new MACSigner(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(missing.serialize()));
    var account = mock(UserAccount.class);
    when(account.getCredentialVersion()).thenReturn(1L);
    when(dao.byId(1)).thenReturn(Optional.of(account));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(signed(now.plusSeconds(60))));
  }

  private String signed(Instant expiry) throws Exception {
    var claims = new JWTClaimsSet.Builder().subject("1").expirationTime(Date.from(expiry)).claim("version", 0).build();
    var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    return jwt.serialize();
  }

  @Test
  void refreshWindowControlsIssuance() {
    Instant now = Instant.parse("2026-11-17T00:00:00Z");
    tokens.setClock(Clock.fixed(now, ZoneOffset.UTC));
    assertNull(tokens.refresh(new TokenService.Identity(1, "USER", 0, now.plusSeconds(901))));
    assertNotNull(tokens.refresh(new TokenService.Identity(1, "USER", 0, now.plusSeconds(900))));
    assertNotNull(tokens.refresh(new TokenService.Identity(1, "USER", 0, now.plusNanos(1))));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.refresh(new TokenService.Identity(1, "USER", 0, now)));
    assertThrows(TokenService.InvalidToken.class, () -> tokens.refresh(new TokenService.Identity(1, "USER", 0, now.minusSeconds(1))));
  }
}
