package com.example.tenisclubreservations.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.example.tenisclubreservations.account.AccountRequest;
import com.example.tenisclubreservations.account.AccountService;
import com.example.tenisclubreservations.account.RoleType;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(LoginRaceIntegrationTest.BlockingConfig.class)
class LoginRaceIntegrationTest {
  @Autowired AccountService accounts;
  @Autowired TokenService tokens;
  @Autowired BlockingEncoder encoder;

  @Test
  void oldPasswordCannotProduceCurrentVersionAfterConcurrentChange() throws Exception {
    var account = accounts.create(new AccountRequest("race", "old-password", RoleType.USER));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var login = executor.submit(() -> tokens.login("race", "old-password"));
      assertTrue(encoder.reached.await(10, TimeUnit.SECONDS));
      try {
        accounts.update(account.id(), new AccountRequest("race", "new-password", RoleType.USER));
      } finally {
        encoder.resume.countDown();
      }
      String oldToken = login.get(10, TimeUnit.SECONDS);
      assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(oldToken));
    }
  }

  @TestConfiguration
  static class BlockingConfig {
    @Bean
    @Primary
    BlockingEncoder blockingEncoder() { return new BlockingEncoder(); }
  }

  static class BlockingEncoder implements PasswordEncoder {
    final CountDownLatch reached = new CountDownLatch(1);
    final CountDownLatch resume = new CountDownLatch(1);
    final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();
    @Override
    public String encode(CharSequence raw) { return delegate.encode(raw); }
    @Override
    public boolean matches(CharSequence raw, String encoded) {
      if (raw.toString().equals("old-password")) {
        reached.countDown();
        try {
          if (!resume.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        }
      }
      return delegate.matches(raw, encoded);
    }
  }
}
