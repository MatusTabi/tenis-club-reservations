package com.example.tenisclubreservations.account;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.MutationGuard;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final AccountDao dao;
  private final MutationGuard guard;
  private final PasswordEncoder encoder;

  @Transactional(readOnly = true)
  public List<AccountResponse> list() { return dao.list().stream().map(AccountResponse::from).toList(); }

  @Transactional(readOnly = true)
  public AccountResponse get(long id) { return AccountResponse.from(find(id)); }

  @Transactional
  public AccountResponse create(AccountRequest input) {
    String username = validate(input);
    guard.lock();
    checkUnique(username, null);
    UserAccount account = new UserAccount(username, encoder.encode(input.password()), dao.role(input.role()));
    dao.save(account);
    return AccountResponse.from(account);
  }

  @Transactional
  public AccountResponse update(long id, AccountRequest input) {
    String username = validate(input);
    guard.lock();
    UserAccount account = find(id);
    checkUnique(username, id);
    if (account.getRole().getRole() == RoleType.ADMIN && input.role() != RoleType.ADMIN && dao.adminCount() == 1) {
      throw new ApiException(HttpStatus.CONFLICT, "Cannot demote the last admin");
    }
    String hash = encoder.matches(input.password(), account.getPasswordHash()) ? null : encoder.encode(input.password());
    account.update(username, hash, dao.role(input.role()));
    return AccountResponse.from(account);
  }

  @Transactional
  public void delete(long id) {
    guard.lock();
    UserAccount account = find(id);
    if (account.getRole().getRole() == RoleType.ADMIN && dao.adminCount() == 1) {
      throw new ApiException(HttpStatus.CONFLICT, "Cannot delete the last admin");
    }
    account.softDelete();
  }

  private UserAccount find(long id) {
    return dao.byId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  private void checkUnique(String username, Long id) {
    if (dao.byUsername(username).filter(a -> !a.getId().equals(id)).isPresent()) {
      throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
    }
  }

  private String validate(AccountRequest input) {
    if (input == null || input.username() == null || input.username().isBlank() || input.username().contains(":") || input.username().trim().length() > 50
        || input.password() == null || input.password().isBlank() || input.password().getBytes(StandardCharsets.UTF_8).length > 72 || input.role() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid account");
    }
    return input.username().trim();
  }
}
