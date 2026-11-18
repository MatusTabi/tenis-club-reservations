package com.example.tenisclubreservations.account;

public record AccountResponse(long id, String username, RoleType role) {
  static AccountResponse from(UserAccount account) {
    return new AccountResponse(account.getId(), account.getUsername(), account.getRole().getRole());
  }
}
