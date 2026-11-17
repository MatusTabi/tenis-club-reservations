package com.example.tenisclubreservations.customer;

import com.example.tenisclubreservations.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
public class Customer extends BaseEntity {
  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String phoneNumber;

  public Customer() {}

  public Customer(String name, String phoneNumber) {
    this.name = name;
    this.phoneNumber = phoneNumber;
  }
}
