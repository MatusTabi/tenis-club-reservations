package com.example.tenisclubreservations.account;

import com.example.tenisclubreservations.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class UserAccount extends BaseEntity {
  @Column(nullable = false, length = 50)
  private String username;

  @Column(unique = true, length = 50)
  private String activeUsername;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false)
  private long credentialVersion;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  private Role role;

  protected UserAccount() {}

  UserAccount(String username, String passwordHash, Role role) {
    this.username = this.activeUsername = username;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  void update(String username, String newHash, Role role) {
    this.username = this.activeUsername = username;
    if (newHash != null) {
      this.passwordHash = newHash;
      credentialVersion++;
    }
    this.role = role;
  }

  @Override
  public void softDelete() {
    super.softDelete();
    activeUsername = null;
  }
}
