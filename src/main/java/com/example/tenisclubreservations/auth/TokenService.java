package com.example.tenisclubreservations.auth;

import com.example.tenisclubreservations.account.AccountDao;
import com.example.tenisclubreservations.account.UserAccount;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {
  private final AccountDao accounts;
  private final PasswordEncoder encoder;
  private final byte[] secret;
  private final Duration lifetime;
  private final Duration refreshWindow;
  private Clock clock = Clock.systemUTC();

  public TokenService(AccountDao accounts, PasswordEncoder encoder,
      @Value("${security.jwt.secret}") String secret,
      @Value("${security.jwt.lifetime}") Duration lifetime,
      @Value("${security.jwt.refresh-window}") Duration refreshWindow) {
    this.accounts = accounts;
    this.encoder = encoder;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.lifetime = lifetime;
    this.refreshWindow = refreshWindow;
    if (this.secret.length < 32 || lifetime.isNegative() || lifetime.isZero()
        || refreshWindow.isNegative() || refreshWindow.isZero() || refreshWindow.compareTo(lifetime) > 0) {
      throw new IllegalArgumentException("Invalid JWT configuration");
    }
  }

  public record Identity(long id, String role, long version, Instant expiry) {}

  void setClock(Clock clock) { this.clock = clock; }

  @Transactional(readOnly = true)
  public String login(String username, String password) {
    if (password == null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) throw new InvalidToken();
    UserAccount account = accounts.byUsername(username.trim()).orElseThrow(InvalidToken::new);
    if (!encoder.matches(password, account.getPasswordHash())) throw new InvalidToken();
    return issue(account.getId(), account.getCredentialVersion());
  }

  @Transactional(readOnly = true)
  public Identity validate(String token) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) throw new InvalidToken();
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Instant expiry = claims.getExpirationTime().toInstant();
      if (!clock.instant().isBefore(expiry)) throw new InvalidToken();
      long id = Long.parseLong(claims.getSubject());
      long version = claims.getLongClaim("version");
      UserAccount account = accounts.byId(id).orElseThrow(InvalidToken::new);
      if (account.getCredentialVersion() != version) throw new InvalidToken();
      return new Identity(id, account.getRole().getRole().name(), version, expiry);
    } catch (Exception e) {
      throw new InvalidToken();
    }
  }

  public String refresh(Identity identity) {
    Instant now = clock.instant();
    if (!now.isBefore(identity.expiry())) throw new InvalidToken();
    if (Duration.between(now, identity.expiry()).compareTo(refreshWindow) > 0) return null;
    return issue(identity.id(), identity.version());
  }

  private String issue(long id, long version) {
    try {
      Instant now = clock.instant();
      JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(Long.toString(id)).issueTime(Date.from(now))
          .expirationTime(Date.from(now.plus(lifetime))).claim("version", version).build();
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to issue token", e);
    }
  }

  public static class InvalidToken extends RuntimeException {}
}
