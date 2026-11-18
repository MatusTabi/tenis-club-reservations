package com.example.tenisclubreservations.account;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "account_role")
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private RoleType role;
}
