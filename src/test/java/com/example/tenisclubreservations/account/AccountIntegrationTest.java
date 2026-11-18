package com.example.tenisclubreservations.account;

import static org.junit.jupiter.api.Assertions.*;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.auth.TokenService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccountIntegrationTest {
  @Autowired AccountService accounts;
  @Autowired TokenService tokens;
  @Autowired JdbcTemplate jdbc;

  @Test
  void usernameReuseAndLastAdminAreEnforced() {
    assertEquals(409, assertThrows(ApiException.class, () -> accounts.delete(1)).getStatusCode().value());
    var user = accounts.create(new AccountRequest("  name  ", "password", RoleType.USER));
    assertEquals("name", user.username());
    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("insert into user_account (created_at, updated_at, username, active_username, password_hash, credential_version, role_id) select current_timestamp, current_timestamp, username, active_username, password_hash, credential_version, role_id from user_account where id = ?", user.id()));
    assertEquals(409, assertThrows(ApiException.class, () -> accounts.create(new AccountRequest("name", "password", RoleType.USER))).getStatusCode().value());
    accounts.delete(user.id());
    assertNotEquals(user.id(), accounts.create(new AccountRequest("name", "password", RoleType.USER)).id());
  }

  @Test
  void concurrentDemotionsKeepAnAdmin() throws Exception {
    var first = accounts.create(new AccountRequest("admin-two", "password", RoleType.ADMIN));
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> { start.await(); try { accounts.delete(1); return true; } catch (ApiException e) { return false; } });
      var b = executor.submit(() -> { start.await(); try { accounts.delete(first.id()); return true; } catch (ApiException e) { return false; } });
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void loginRacingPasswordChangeCannotLeaveAnOldCredentialTokenValid() throws Exception {
    var account = accounts.create(new AccountRequest("race-login", "old-password", RoleType.USER));
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var login = executor.submit(() -> {
        start.await();
        try { return tokens.login("race-login", "old-password"); }
        catch (TokenService.InvalidToken e) { return null; }
      });
      var update = executor.submit(() -> {
        start.await();
        accounts.update(account.id(), new AccountRequest("race-login", "new-password", RoleType.USER));
        return null;
      });
      start.countDown();
      String token = login.get(10, TimeUnit.SECONDS);
      update.get(10, TimeUnit.SECONDS);
      if (token != null) assertThrows(TokenService.InvalidToken.class, () -> tokens.validate(token));
      assertNotNull(tokens.login("race-login", "new-password"));
    }
  }

  @Test
  void rejectsInvalidAccountBoundaries() {
    assertEquals(400, assertThrows(ApiException.class, () -> accounts.create(new AccountRequest(" ", "password", RoleType.USER))).getStatusCode().value());
    assertEquals(400, assertThrows(ApiException.class, () -> accounts.create(new AccountRequest("x".repeat(51), "password", RoleType.USER))).getStatusCode().value());
    assertEquals(400, assertThrows(ApiException.class, () -> accounts.create(new AccountRequest("valid", "x".repeat(73), RoleType.USER))).getStatusCode().value());
    assertEquals(400, assertThrows(ApiException.class, () -> accounts.create(new AccountRequest("valid", "password", null))).getStatusCode().value());
  }
}
